/*******************************************************************************
 * Copyright (c) 2000, 2011 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.utils.spawner;

import java.io.*;
import java.util.StringTokenizer;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import com.google.eclipse.elt.pty.PtyPlugin;

public class Spawner extends Process {
  static final String LIBRARY_NAME = "gspawner";

  public int NOOP = 0;
  public int HUP = 1;
  public int KILL = 9;
  public int TERM = 15;

  /**
   * On Windows, what this does is far from easy to explain. Some of the logic is in the JNI code, some in the
   * spawner.exe code.
   *
   * <ul>
   * <li>If the process this is being raised against was launched by us (the Spawner)
   * <ul>
   * <li>If the process is a cygwin program (has the cygwin1.dll loaded), then issue a 'kill -SIGINT'. If the 'kill'
   * utility isn't available, send the process a CTRL-C
   * <li>If the process is <i>not</i> a cygwin program, send the process a CTRL-C
   * </ul>
   * <li>If the process this is being raised against was <i>not</i> launched by us, use DebugBreakProcess to interrupt
   * it (sending a CTRL-C is easy only if we share a console with the target process)
   * </ul>
   *
   * On non-Windows, raising this just raises a POSIX SIGINT
   *
   */
  public int INT = 2;

  /**
   * A fabricated signal number for use on Windows only. Tells the starter program to send a CTRL-C regardless of
   * whether the process is a Cygwin one or not.
   *
   * @since 5.2
   */
  public int CTRLC = 1000; // arbitrary high number to avoid collision

  int pid = 0;
  int status;
  final int[] fChannels = new int[3];
  boolean isDone;
  OutputStream out;
  InputStream in;
  InputStream err;
  private PTY fPty;

  public Spawner(String command, boolean bNoRedirect) throws IOException {
    StringTokenizer tokenizer = new StringTokenizer(command);
    String[] cmdarray = new String[tokenizer.countTokens()];
    for (int n = 0; tokenizer.hasMoreTokens(); n++) {
      cmdarray[n] = tokenizer.nextToken();
    }
    if (bNoRedirect) {
      exec_detached(cmdarray, new String[0], ".");
    } else {
      exec(cmdarray, new String[0], ".");
    }
  }

  protected Spawner(String[] command, String[] environment, File workingDirectory) throws IOException {
    String dirpath = ".";
    if (workingDirectory != null) {
      dirpath = workingDirectory.getAbsolutePath();
    }
    exec(command, environment, dirpath);
  }

  protected Spawner(String[] cmdarray, String[] envp, File dir, PTY pty) throws IOException {
    String dirpath = ".";
    if (dir != null) {
      dirpath = dir.getAbsolutePath();
    }
    fPty = pty;
    exec_pty(cmdarray, envp, dirpath, pty);
  }

  protected Spawner(String command) throws IOException {
    this(command, null);
  }

  protected Spawner(String[] command) throws IOException {
    this(command, null);
  }

  protected Spawner(String[] command, String[] environment) throws IOException {
    this(command, environment, null);
  }

  protected Spawner(String command, String[] environment) throws IOException {
    this(command, environment, null);
  }

  protected Spawner(String command, String[] environment, File workingDirectory) throws IOException {
    StringTokenizer tokenizer = new StringTokenizer(command);
    String[] cmdarray = new String[tokenizer.countTokens()];
    for (int n = 0; tokenizer.hasMoreTokens(); n++) {
      cmdarray[n] = tokenizer.nextToken();
    }
    String dirpath = ".";
    if (workingDirectory != null) {
      dirpath = workingDirectory.getAbsolutePath();
    }
    exec(cmdarray, environment, dirpath);
  }

  @Override protected void finalize() throws Throwable {
    closeUnusedStreams();
  }

  /**
   * See java.lang.Process#getInputStream (); The client is responsible for closing the stream explicitly.
   **/
  @Override public synchronized InputStream getInputStream() {
    if (null == in) {
      if (fPty != null) {
        in = fPty.getInputStream();
      } else {
        in = new SpawnerInputStream(fChannels[1]);
      }
    }
    return in;
  }

  /**
   * See java.lang.Process#getOutputStream (); The client is responsible for closing the stream explicitly.
   **/
  @Override public synchronized OutputStream getOutputStream() {
    if (null == out) {
      if (fPty != null) {
        out = fPty.getOutputStream();
      } else {
        out = new SpawnerOutputStream(fChannels[0]);
      }
    }
    return out;
  }

  /**
   * See java.lang.Process#getErrorStream (); The client is responsible for closing the stream explicitly.
   **/
  @Override public synchronized InputStream getErrorStream() {
    if (null == err) {
      if (fPty != null && !fPty.isConsole()) {
        // If PTY is used and it's not in "Console" mode, then stderr is redirected to the PTY's output stream.
        // Therefore, return a dummy stream for error stream.
        err = new InputStream() {
          @Override public int read() {
            return -1;
          }
        };
      } else {
        err = new SpawnerInputStream(fChannels[2]);
      }
    }
    return err;
  }

  /**
   * See java.lang.Process#waitFor ();
   **/
  @Override public synchronized int waitFor() throws InterruptedException {
    while (!isDone) {
      wait();
    }

    // For situations where the user does not call destroy(),
    // we try to kill the streams that were not used here.
    // We check for streams that were not created, we create
    // them to attach to the pipes, and then we close them
    // to release the pipes.
    // Streams that were created by the client need to be
    // closed by the client itself.
    //
    // But 345164
    closeUnusedStreams();
    return status;
  }

  /**
   * See java.lang.Process#exitValue ();
   **/
  @Override public synchronized int exitValue() {
    if (!isDone) {
      throw new IllegalThreadStateException("Process not Terminated");
    }
    return status;
  }

  /**
   * See java.lang.Process#destroy ();
   *
   * Clients are responsible for explicitly closing any streams that they have requested through getErrorStream(),
   * getInputStream() or getOutputStream()
   **/
  @Override public synchronized void destroy() {
    // Sends the TERM
    terminate();

    // Close the streams on this side.
    //
    // We only close the streams that were
    // never used by any client.
    // So, if the stream was not created yet,
    // we create it ourselves and close it
    // right away, so as to release the pipe.
    // Note that even if the stream was never
    // created, the pipe has been allocated in
    // native code, so we need to create the
    // stream and explicitly close it.
    //
    // We don't close streams the clients have
    // created because we don't know when the
    // client will be finished using them.
    // It is up to the client to close those
    // streams.
    //
    // But 345164
    closeUnusedStreams();

    // Grace before using the heavy gone.
    if (!isDone) {
      try {
        wait(1000);
      } catch (InterruptedException e) {
      }
    }
    if (!isDone) {
      kill();
    }
  }

  public int interrupt() {
    return raise(pid, INT);
  }

  public int interruptCTRLC() {
    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      return raise(pid, CTRLC);
    }
    return interrupt();
  }

  public int hangup() {
    return raise(pid, HUP);
  }

  public int kill() {
    return raise(pid, KILL);
  }

  public int terminate() {
    return raise(pid, TERM);
  }

  public boolean isRunning() {
    return (raise(pid, NOOP) == 0);
  }

  private void exec(String[] cmdarray, String[] envp, String dirpath) throws IOException {
    String command = cmdarray[0];
    SecurityManager s = System.getSecurityManager();
    if (s != null) {
      s.checkExec(command);
    }
    if (envp == null) {
      envp = new String[0];
    }
    Reaper reaper = new Reaper(cmdarray, envp, dirpath);
    reaper.setDaemon(true);
    reaper.start();
    // Wait until the subprocess is started or error.
    synchronized (this) {
      while (pid == 0) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    // Check for errors.
    if (pid == -1) {
      throw new IOException(reaper.getErrorMessage());
    }
  }

  private void exec_pty(String[] command, String[] environment, String workingDirectory, PTY pty) throws IOException {
    String cmd = command[0];
    SecurityManager s = System.getSecurityManager();
    if (s != null) {
      s.checkExec(cmd);
    }
    if (environment == null) {
      environment = new String[0];
    }
    final String slaveName = pty.getSlaveName();
    final int masterFD = pty.getMasterFD().getFD();
    final boolean console = pty.isConsole();
    // int fdm = pty.get
    Reaper reaper = new Reaper(command, environment, workingDirectory) {
      @Override int execute(String[] cmd, String[] env, String dir, int[] channels) throws IOException {
        return exec2(cmd, env, dir, channels, slaveName, masterFD, console);
      }
    };
    reaper.setDaemon(true);
    reaper.start();
    // Wait until the subprocess is started or error.
    synchronized (this) {
      while (pid == 0) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    if (pid == -1) {
      throw new IOException("Exec_tty error:" + reaper.getErrorMessage());
    }
  }

  public void exec_detached(String[] cmdarray, String[] envp, String dirpath) throws IOException {
    String command = cmdarray[0];
    SecurityManager s = System.getSecurityManager();
    if (s != null) {
      s.checkExec(command);
    }
    if (envp == null) {
      envp = new String[0];
    }
    pid = exec1(cmdarray, envp, dirpath);
    if (pid == -1) {
      throw new IOException("Exec error");
    }
    fChannels[0] = -1;
    fChannels[1] = -1;
    fChannels[2] = -1;
  }

  private synchronized void closeUnusedStreams() {
    try {
      if (null == err) {
        getErrorStream().close();
      }
    } catch (IOException e) {}
    try {
      if (null == in) {
        getInputStream().close();
      }
    } catch (IOException e) {}
    try {
      if (null == out) {
        getOutputStream().close();
      }
    } catch (IOException e) {}
  }

  native int exec0(String[] cmdarray, String[] envp, String dir, int[] chan) throws IOException;

  native int exec1(String[] cmdarray, String[] envp, String dir) throws IOException;

  native int exec2(String[] cmdarray, String[] envp, String dir, int[] chan, String slaveName, int masterFD,
      boolean console) throws IOException;

  public native int raise(int processID, int sig);

  native int waitFor(int processID);

  static {
    System.loadLibrary(LIBRARY_NAME);
  }

  // Spawn a thread to handle the forking and waiting.
  // We do it this way because on linux the SIGCHLD is send to the one thread. So do the forking and then wait in the
  // same thread.
  class Reaper extends Thread {
    String[] command;
    String[] environment;
    String dirPath;
    volatile Throwable exception;

    public Reaper(String[] command, String[] environment, String dirPath) {
      super("Spawner Reaper");
      this.command = command;
      this.environment = environment;
      this.dirPath = dirPath;
      exception = null;
    }

    int execute(String[] cmdarray, String[] envp, String dir, int[] channels) throws IOException {
      return exec0(cmdarray, envp, dir, channels);
    }

    @Override public void run() {
      try {
        pid = execute(command, environment, dirPath, fChannels);
      } catch (Exception e) {
        pid = -1;
        exception = e;
      }
      // Tell spawner that the process started.
      synchronized (Spawner.this) {
        Spawner.this.notifyAll();
      }
      if (pid != -1) {
        // Sync with spawner and notify when done.
        status = waitFor(pid);
        synchronized (Spawner.this) {
          isDone = true;
          Spawner.this.notifyAll();
        }
      }
    }

    public String getErrorMessage() {
      final String reason = exception != null ? exception.getMessage() : "Unknown reason";
      return NLS.bind(PtyPlugin.getResourceString("Util.error.cannotRun"), command[0], reason);
    }
  }
}
