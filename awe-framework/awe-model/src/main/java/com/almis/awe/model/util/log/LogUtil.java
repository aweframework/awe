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

  private static final String TOTAL_TIME_STRING = "Total time: ";
  private static final String SECONDS = "s";
  private static final String END_COLOR = "\u001B[0m";
  private static final String RED_COLOR = "\u001B[31m";
  private static final String ORANGE_COLOR = "\u001B[38:5:208m";
  private static final String YELLOW_COLOR = "\u001B[33m";

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
  public static String getTotalTime(List<Long> timeLapse) {
    double time = getElapsed(timeLapse, 0, timeLapse.size() - 1);
    if (time > 10) {
      // En rojo
      return RED_COLOR + TOTAL_TIME_STRING + time + SECONDS + " WARNING!" + END_COLOR;
    } else if (time > 5) {
      // En naranja
      return ORANGE_COLOR + TOTAL_TIME_STRING + time + SECONDS + END_COLOR;
    } else if (time > 2) {
      // En amarillo
      return YELLOW_COLOR + TOTAL_TIME_STRING + time + SECONDS + END_COLOR;
    } else {
      // En blanco
      return TOTAL_TIME_STRING + time + SECONDS;
    }
  }
}
