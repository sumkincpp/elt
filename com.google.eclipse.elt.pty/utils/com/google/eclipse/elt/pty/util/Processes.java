package com.google.eclipse.elt.pty.util;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility methods related to processes.
 */
public final class Processes {
  /**
   * Starts a new process.
   * @param command the command to execute.
   * @param environment the environment variables.
   * @param workingDirectory the working directory.
   * @return the new process.
   * @throws IOException if something went wrong.
   */
  public static Process executeInPty(
      String[] command, Map<String, String> environment, File workingDirectory) throws IOException {
    PTY pty = new PTY(true);
    ProcessFactory processFactory = ProcessFactory.getFactory();
    return processFactory.exec(command, toArray(environment), workingDirectory, pty);
  }

  private static String[] toArray(Map<String, String> environmentMap) {
    List<String> environment = new ArrayList<String>();
    for (Entry<String, String> entry : environmentMap.entrySet()) {
      environment.add(entry.getKey() + "=" + entry.getValue());
    }
    return environment.toArray(new String[environment.size()]);
  }

  private Processes() {}
}
