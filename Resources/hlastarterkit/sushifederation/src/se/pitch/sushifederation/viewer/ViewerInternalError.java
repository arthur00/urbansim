 /** Used to signal internal errors in federate
 */

 package se.pitch.sushifederation.viewer;

public class ViewerInternalError extends Exception {
  public ViewerInternalError(String s) {
    super(s);
  }
} 
