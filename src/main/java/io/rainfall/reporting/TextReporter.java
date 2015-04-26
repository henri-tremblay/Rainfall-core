/*
 * Copyright 2014 Aurélien Broszniowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rainfall.reporting;

import io.rainfall.Reporter;
import io.rainfall.statistics.Statistics;
import io.rainfall.statistics.StatisticsHolder;
import io.rainfall.statistics.StatisticsPeek;
import io.rainfall.statistics.StatisticsPeekHolder;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;


/**
 * report the statistics to the text console
 *
 * @author Aurelien Broszniowski
 */

public class TextReporter<E extends Enum<E>> extends Reporter<E> {

  private static final String FORMAT = "%-15s %-15s %12s %10s %10s";
  //  private static final String FORMAT = "%-15s %-7s %12s %10s %10s %10s %10s %10s";
  private static final NumberFormat nf = NumberFormat.getInstance();
  private String CRLF = System.getProperty("line.separator");
  private Calendar calendar = GregorianCalendar.getInstance(TimeZone.getDefault());
  private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

  @Override
  public void report(final StatisticsPeekHolder<E> statisticsHolder) {
    StringBuilder sb = new StringBuilder();
    StatisticsPeek<E> totalStatisticsPeeks = statisticsHolder.getTotalStatisticsPeeks();
    Set<String> keys = statisticsHolder.getStatisticsPeeksNames();

    sb.append("===================================================== PERIODIC ==========================================")
        .append(CRLF);
    sb.append(String.format(FORMAT, "Cache", "Type", "Txn_Count", "TPS", "Avg_Lat"))
//    sb.append(String.format(FORMAT, "Cache", "Type", "Txn_Count", "TPS", "Avg_Lat", "Min_Lat", "Max_Lat", "TotalExceptionCount"))
        .append(CRLF);
    sb.append("==========================================================================================================")
        .append(CRLF);

    for (String key : keys) {
      StatisticsPeek<E> statisticsPeeks = statisticsHolder.getStatisticsPeeks(key);
      logPeriodicStats(sb, key, statisticsPeeks);
    }

    if (totalStatisticsPeeks != null)
      logPeriodicStats(sb, "ALL", totalStatisticsPeeks);

    System.out.println(sb.toString());
  }

  @Override
  public void summarize(final StatisticsHolder<E> statisticsHolder) {
//    sb.append(String.format(FORMAT, "Cache", "Type", "Txn_Count", "TPS", "Avg_Lat"))
    Set<String> observedEntities = statisticsHolder.getStatisticsKeys();
    for (String observedEntity : observedEntities) {
      System.out.println("***************** Observed entity is : " + observedEntity);
      Statistics<E> statistics = statisticsHolder.getStatistics(observedEntity);
      ConcurrentHashMap<Enum, Histogram> histograms = statistics.getHistograms();
      for (Enum key : histograms.keySet()) {
        System.out.println("*************> " + key);
        Histogram histogram = histograms.get(key);
        histogram.outputPercentileDistribution(System.out, 5, 1000000d, true);

        System.out.println("*****--------- Mean Response time, Max Response time, Std dev "); //TODO
        System.out.println("*****--------- Mean TPS, Max TPS"); //TODO get it from cumulative

        System.out.println("*****--------- Percentile of latencies");
        AbstractHistogram.Percentiles values = histogram.percentiles(5);
        for (HistogramIterationValue value : values) {
          System.out.println(value.toString());
        }

        System.out.println("*****--------- Periodic lat (done by report()"); // TODO : remove cumulative
        System.out.println("*****--------- Periodic TPS (done by report()"); // TODO : remove cumulative
      }
    }
  }

  private void logPeriodicStats(StringBuilder sb, String name, StatisticsPeek<E> peek) {
    sb.append(formatTimestampInMs(peek.getTimestamp())).append(CRLF);
    Enum<E>[] keys = peek.getKeys();
    for (Enum<E> key : keys) {
      sb.append(String.format(FORMAT,
          name,
          key.name(),
          nf.format(peek.getPeriodicCounters(key)),
          nf.format(peek.getPeriodicTps(key)),
          nf.format(peek.getPeriodicAverageLatencyInMs(key))
      )).append(CRLF);
    }
    sb.append(String.format(FORMAT,
        name,
        "TOTAL",
        nf.format(peek.getSumOfPeriodicCounters()),
        nf.format(peek.getSumOfPeriodicTps()),
        nf.format(peek.getAverageOfPeriodicAverageLatencies())
    )).append(CRLF);
  }

  private String formatTimestampInMs(final long timestamp) {
    calendar.setTime(new Date(timestamp));
    return sdf.format(calendar.getTime());
  }
}
