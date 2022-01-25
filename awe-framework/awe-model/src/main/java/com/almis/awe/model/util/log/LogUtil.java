package com.almis.awe.model.util.log;


import java.util.ArrayList;
import java.util.List;

/**
 * LogUtil Class
 * Returns an instance of logger.
 *
 * @author Pablo VIDAL - 21/Ene/2022
 */
public class LogUtil {

  /**
   * Constructor
   */
  private LogUtil() {
  }

  /**
   * Generate a time lapse
   *
   * @return Time lapse
   */
  public static List<Long> prepareTimeLapse() {
    List<Long> timeLapse = new ArrayList<>();
    checkpoint(timeLapse);
    return timeLapse;
  }

  /**
   * Store a checkpoint in the timelapse
   *
   * @param timeLapse Time lapse
   */
  public static void checkpoint(List<Long> timeLapse) {
    timeLapse.add(System.currentTimeMillis());
  }

  /**
   * Return the elapsed time between two code points
   *
   * @param timeLapse  Time lapse
   * @param startPoint start point
   * @param endPoint   end point
   * @return Elapsed time
   */
  public static double getElapsed(List<Long> timeLapse, int startPoint, int endPoint) {
    long startTime = timeLapse.get(startPoint);
    long endTime = timeLapse.get(endPoint);
    return ((double) (endTime - startTime)) / 1000;
  }

  /**
   * Return the elapsed time in a point
   *
   * @param timeLapse Time lapse
   * @param point     point to retrieve (starting from 1)
   * @return Elapsed time
   */
  public static double getElapsed(List<Long> timeLapse, int point) {
    return getElapsed(timeLapse, point - 1, point);
  }


  /**
   * Return the total elapsed time
   *
   * @param timeLapse Time lapse
   * @return Elapsed time
   */
  public static double getTotalTime(List<Long> timeLapse) {
    return getElapsed(timeLapse, 0, timeLapse.size() - 1);
  }
}
