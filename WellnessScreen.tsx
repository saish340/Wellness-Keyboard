import React, { useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
} from 'react-native';
import {
  VictoryAxis,
  VictoryChart,
  VictoryLine,
  VictoryScatter,
} from 'victory-native';
import Animated, { FadeInDown, FadeInUp } from 'react-native-reanimated';

type DailyWellnessPoint = {
  day: string;
  score: number;
};

type MetricCardProps = {
  title: string;
  value: string;
  subtitle: string;
  trend?: 'up' | 'down' | 'flat';
  progress: number;
};

const generateMockData = (): DailyWellnessPoint[] => {
  const scores = [
    0.18, 0.22, 0.31, 0.28, 0.36, 0.42, 0.45, 0.39, 0.52, 0.58,
    0.64, 0.71, 0.76, 0.69, 0.62, 0.55, 0.49, 0.43, 0.38, 0.33,
    0.29, 0.35, 0.41, 0.47, 0.54, 0.61, 0.66, 0.72, 0.68, 0.59,
  ];

  return scores.map((score, index) => ({
    day: `${index + 1}`,
    score,
  }));
};

const getSegmentColor = (score: number): string => {
  if (score < 0.4) {
    return '#2EAD67';
  }

  if (score <= 0.7) {
    return '#D9A441';
  }

  return '#D14B4B';
};

const buildSegments = (data: DailyWellnessPoint[]) => {
  const segments: Array<{
    key: string;
    data: DailyWellnessPoint[];
    color: string;
  }> = [];

  for (let index = 0; index < data.length - 1; index += 1) {
    const startPoint = data[index];
    const endPoint = data[index + 1];
    const segmentScore = (startPoint.score + endPoint.score) / 2;

    segments.push({
      key: `${startPoint.day}-${endPoint.day}`,
      data: [startPoint, endPoint],
      color: getSegmentColor(segmentScore),
    });
  }

  return segments;
};

const getWellnessStatus = (score: number) => {
  if (score < 0.4) {
    return { label: 'Steady', color: '#2EAD67', backgroundColor: '#EAF7EF' };
  }

  if (score <= 0.7) {
    return { label: 'Balanced', color: '#D9A441', backgroundColor: '#FFF6E3' };
  }

  return { label: 'Elevated', color: '#D14B4B', backgroundColor: '#FDECEC' };
};

const MetricCard = ({ title, value, subtitle, trend, progress }: MetricCardProps) => {
  const trendSymbol = trend === 'up' ? '↗' : trend === 'down' ? '↘' : '•';
  const trendColor = trend === 'up' ? '#2EAD67' : trend === 'down' ? '#D14B4B' : '#6B7280';

  return (
    <View style={styles.metricCard}>
      <View style={styles.metricHeaderRow}>
        <Text style={styles.metricTitle}>{title}</Text>
        <Text style={[styles.metricTrend, { color: trendColor }]}>{trendSymbol}</Text>
      </View>
      <Text style={styles.metricValue}>{value}</Text>
      <Text style={styles.metricSubtitle}>{subtitle}</Text>
      <View style={styles.progressTrack}>
        <View style={[styles.progressFill, { width: `${Math.min(Math.max(progress, 0), 1) * 100}%`, backgroundColor: trendColor }]} />
      </View>
    </View>
  );
};

const WellnessScreen = () => {
  const { width } = useWindowDimensions();

  const chartData = useMemo(() => generateMockData(), []);
  const chartSegments = useMemo(() => buildSegments(chartData), [chartData]);
  const chartWidth = Math.min(width - 32, 420);
  const chartHeight = 280;

  const latestScore = chartData[chartData.length - 1]?.score ?? 0;
  const wellnessStatus = getWellnessStatus(latestScore);
  const weeklyAverageScore = chartData.slice(-7).reduce((sum, point) => sum + point.score, 0) / 7;

  const averageTypingSpeedThisWeek = '48 wpm';
  const backspaceTrend = 'Down 12%';
  const sleepHourTypingRatio = '18%';

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Animated.View entering={FadeInDown.duration(500)} style={styles.header}>
        <View style={styles.headerTopRow}>
          <View>
            <Text style={styles.kicker}>Behavioral wellness</Text>
            <Text style={styles.title}>30-Day Wellness Baseline</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: wellnessStatus.backgroundColor }]}>
            <View style={[styles.statusDot, { backgroundColor: wellnessStatus.color }]} />
            <Text style={[styles.statusText, { color: wellnessStatus.color }]}>{wellnessStatus.label}</Text>
          </View>
        </View>
        <Text style={styles.subtitle}>
          A calm snapshot of typing patterns, cognitive load, and behavioral wellness signals.
        </Text>
      </Animated.View>

      <Animated.View entering={FadeInUp.delay(100).duration(500)} style={styles.summaryCard}>
        <View style={styles.summaryRow}>
          <View>
            <Text style={styles.summaryLabel}>Weekly wellness score</Text>
            <Text style={styles.summaryValue}>{Math.round(weeklyAverageScore * 100)}%</Text>
          </View>
          <View style={styles.summaryRing}>
            <Text style={styles.summaryRingValue}>{Math.round(latestScore * 100)}</Text>
            <Text style={styles.summaryRingCaption}>score</Text>
          </View>
        </View>
        <View style={styles.summaryTrack}>
          <View
            style={[
              styles.summaryFill,
              {
                width: `${Math.min(Math.max(weeklyAverageScore * 100, 12), 100)}%`,
                backgroundColor: wellnessStatus.color,
              },
            ]}
          />
        </View>
      </Animated.View>

      <Animated.View entering={FadeInUp.delay(180).duration(500)} style={styles.chartCard}>
        <View style={styles.chartCardHeader}>
          <View>
            <Text style={styles.chartTitle}>Wellness score trend</Text>
            <Text style={styles.chartCaption}>Daily anomaly score over 30 days</Text>
          </View>
          <Text style={styles.chartCaption}>30 days</Text>
        </View>

        <VictoryChart
          width={chartWidth}
          height={chartHeight}
          padding={{ top: 24, bottom: 52, left: 52, right: 20 }}
          domain={{ y: [0, 1] }}
          scale={{ x: 'linear', y: 'linear' }}
        >
          <VictoryAxis
            tickValues={[1, 6, 11, 16, 21, 26, 30]}
            tickFormat={(tick) => `${tick}`}
            style={axisStyles}
          />
          <VictoryAxis
            dependentAxis
            tickValues={[0, 0.25, 0.5, 0.75, 1]}
            tickFormat={(tick) => `${Math.round(Number(tick) * 100)}`}
            style={axisStyles}
          />

          {chartSegments.map((segment) => (
            <VictoryLine
              key={segment.key}
              data={segment.data.map((point) => ({ x: Number(point.day), y: point.score }))}
              interpolation="monotoneX"
              style={{
                data: {
                  stroke: segment.color,
                  strokeWidth: 4,
                  strokeLinecap: 'round',
                },
              }}
            />
          ))}

          <VictoryScatter
            data={chartData.map((point) => ({ x: Number(point.day), y: point.score }))}
            size={3.5}
            style={{
              data: {
                fill: '#FFFFFF',
                stroke: '#A3A3A3',
                strokeWidth: 1.5,
              },
            }}
          />
        </VictoryChart>

        <View style={styles.legendRow}>
          <LegendPill label="Steady" color="#2EAD67" />
          <LegendPill label="Balanced" color="#D9A441" />
          <LegendPill label="Elevated" color="#D14B4B" />
        </View>
      </Animated.View>

      <View style={styles.metricsGrid}>
        <Animated.View entering={FadeInUp.delay(240).duration(500)} style={styles.metricWrapper}>
          <MetricCard
            title="Average typing speed this week"
            value={averageTypingSpeedThisWeek}
            subtitle="Based on recent daily sessions"
            trend="up"
            progress={0.72}
          />
        </Animated.View>
        <Animated.View entering={FadeInUp.delay(320).duration(500)} style={styles.metricWrapper}>
          <MetricCard
            title="Backspace rate trend"
            value={backspaceTrend}
            subtitle="Compared with the prior week"
            trend="down"
            progress={0.42}
          />
        </Animated.View>
        <Animated.View entering={FadeInUp.delay(400).duration(500)} style={styles.metricWrapper}>
          <MetricCard
            title="Sleep-hour typing ratio"
            value={sleepHourTypingRatio}
            subtitle="Typing activity between 11pm and 4am"
            trend="flat"
            progress={0.18}
          />
        </Animated.View>
      </View>

      <Animated.View entering={FadeInUp.delay(460).duration(500)} style={styles.infoPanel}>
        <Text style={styles.infoTitle}>Why this matters</Text>
        <Text style={styles.infoText}>
          This dashboard analyzes behavioral signals related to cognitive load and behavioral fatigue.
        </Text>
      </Animated.View>

      <Animated.Text entering={FadeInUp.delay(520).duration(500)} style={styles.disclaimer}>
        This is not a medical tool. Consult a professional if concerned.
      </Animated.Text>
    </ScrollView>
  );
};

const LegendPill = ({ label, color }: { label: string; color: string }) => (
  <View style={styles.legendPill}>
    <View style={[styles.legendDot, { backgroundColor: color }]} />
    <Text style={styles.legendText}>{label}</Text>
  </View>
);

const axisStyles = {
  axis: { stroke: '#E5E7EB' },
  grid: { stroke: '#F3F4F6', strokeDasharray: '4 4' },
  ticks: { stroke: '#CBD5E1', size: 4 },
  tickLabels: { fill: '#6B7280', fontSize: 11, fontFamily: 'System' },
};

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  content: {
    paddingHorizontal: 18,
    paddingTop: 22,
    paddingBottom: 30,
  },
  header: {
    marginBottom: 14,
  },
  headerTopRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  kicker: {
    fontSize: 12,
    letterSpacing: 1.4,
    textTransform: 'uppercase',
    color: '#6B7280',
    marginBottom: 8,
  },
  title: {
    fontSize: 28,
    lineHeight: 34,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 6,
    fontFamily: 'System',
  },
  subtitle: {
    fontSize: 15,
    lineHeight: 23,
    color: '#6B7280',
    fontFamily: 'System',
    marginTop: 8,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 999,
    marginTop: 2,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  statusText: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.2,
    fontFamily: 'System',
  },
  summaryCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 24,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#111827',
    shadowOpacity: 0.06,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 5 },
    elevation: 3,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  summaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
    marginBottom: 14,
  },
  summaryLabel: {
    fontSize: 13,
    lineHeight: 18,
    color: '#6B7280',
    fontFamily: 'System',
    marginBottom: 4,
  },
  summaryValue: {
    fontSize: 30,
    lineHeight: 34,
    fontWeight: '800',
    color: '#111827',
    fontFamily: 'System',
  },
  summaryRing: {
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 10,
    borderColor: '#EEF2F7',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FAFAFA',
  },
  summaryRingValue: {
    fontSize: 18,
    fontWeight: '800',
    color: '#111827',
    fontFamily: 'System',
    lineHeight: 20,
  },
  summaryRingCaption: {
    fontSize: 10,
    color: '#6B7280',
    fontFamily: 'System',
    marginTop: 1,
  },
  summaryTrack: {
    height: 8,
    borderRadius: 999,
    backgroundColor: '#EEF2F7',
    overflow: 'hidden',
  },
  summaryFill: {
    height: '100%',
    borderRadius: 999,
  },
  chartCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 24,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#111827',
    shadowOpacity: 0.07,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 5 },
    elevation: 3,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  chartCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  chartTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: '#111827',
    fontFamily: 'System',
  },
  chartCaption: {
    fontSize: 12,
    color: '#6B7280',
    fontFamily: 'System',
  },
  legendRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 4,
  },
  legendPill: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 999,
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#F9FAFB',
    borderWidth: 1,
    borderColor: '#EEF2F7',
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  legendText: {
    fontSize: 12,
    color: '#374151',
    fontFamily: 'System',
    fontWeight: '600',
  },
  metricsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: 16,
    gap: 12,
  },
  metricWrapper: {
    width: '100%',
  },
  metricCard: {
    width: '100%',
    minHeight: 146,
    backgroundColor: '#FFFFFF',
    borderRadius: 22,
    padding: 16,
    shadowColor: '#111827',
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 2,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  metricHeaderRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  metricTitle: {
    flex: 1,
    fontSize: 13,
    lineHeight: 18,
    color: '#374151',
    fontWeight: '600',
    fontFamily: 'System',
    paddingRight: 8,
  },
  metricTrend: {
    fontSize: 20,
    fontWeight: '700',
    lineHeight: 22,
    fontFamily: 'System',
  },
  metricValue: {
    fontSize: 28,
    lineHeight: 32,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 10,
    fontFamily: 'System',
  },
  metricSubtitle: {
    fontSize: 12,
    lineHeight: 18,
    color: '#6B7280',
    fontFamily: 'System',
    marginBottom: 12,
  },
  progressTrack: {
    height: 8,
    borderRadius: 999,
    backgroundColor: '#EEF2F7',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: 999,
  },
  infoPanel: {
    backgroundColor: '#F9FAFB',
    borderRadius: 20,
    padding: 16,
    borderWidth: 1,
    borderColor: '#EEF2F7',
    marginBottom: 18,
  },
  infoTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 6,
    fontFamily: 'System',
  },
  infoText: {
    fontSize: 14,
    lineHeight: 21,
    color: '#4B5563',
    fontFamily: 'System',
  },
  disclaimer: {
    fontSize: 12,
    lineHeight: 18,
    color: '#9CA3AF',
    textAlign: 'center',
    paddingHorizontal: 18,
    fontFamily: 'System',
  },
});

export default WellnessScreen;
