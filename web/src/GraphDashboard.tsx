import React, { useMemo } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

type ResultEntry = {
  // Original detailed format
  label?: string;
  payload_bytes?: number;
  throughput_mbps: number;
  rtts_ms?: number[];
  // Simpler format we also accept
  payload?: number;
  avg_rtt_ms?: number;
};

type ResultFile = {
  protocol: string;
  host: string;
  server_name?: string;
  payload_sizes: number[];
  results: ResultEntry[];
  _file?: string;
};

type Series = {
  id: string;
  serverName: string;
  protocol: string;
  fileName: string;
  points: { payload: number; avgRtt: number; throughput: number }[];
};

function median(values: number[]): number {
  const vs = values.slice().sort((a, b) => a - b);
  if (vs.length === 0) return NaN;
  const mid = Math.floor(vs.length / 2);
  if (vs.length % 2 === 1) return vs[mid];
  return (vs[mid - 1] + vs[mid]) / 2;
}

function buildSeries(datasets: any[]): Series[] {
  const series: Series[] = [];
  for (const raw of datasets as ResultFile[]) {
    if (!raw || !raw.results) continue;
    const protocol = (raw.protocol || "?").toUpperCase();
    const host = raw.host || "?";
    const serverName = raw.server_name || host;
    const fileName = raw._file || "unknown.json";

    const points: Series["points"] = [];
    for (const r of raw.results) {
      let avgRtt: number;
      if (Array.isArray(r.rtts_ms) && r.rtts_ms.length > 0) {
        let rtts = r.rtts_ms;
        if (protocol === "UDP") {
          rtts = rtts.filter((v) => v >= 0);
        }
        avgRtt = median(rtts);
      } else if (typeof r.avg_rtt_ms === "number") {
        avgRtt = r.avg_rtt_ms;
      } else {
        avgRtt = NaN;
      }
      points.push({
        payload: r.payload_bytes ?? r.payload ?? 0,
        avgRtt,
        throughput: r.throughput_mbps,
      });
    }
    points.sort((a, b) => a.payload - b.payload);

    const id = `${protocol}-${serverName}-${fileName}`;
    series.push({
      id,
      serverName,
      protocol,
      fileName,
      points,
    });
  }
  return series;
}

export const GraphDashboard: React.FC<{ datasets: any[] }> = ({ datasets }) => {
  const series = useMemo(() => buildSeries(datasets), [datasets]);

  if (series.length === 0) {
    return (
      <div
        style={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "#9ca3af",
          fontSize: 14,
        }}
      >
        Waiting for data from /plot_results.json. Once your Java client writes results there, graphs will appear here.
      </div>
    );
  }

  return (
    <div
      style={{
        flex: 1,
        display: "flex",
        flexDirection: "column",
        gap: 16,
      }}
    >
      <div
        style={{
          marginBottom: 4,
          fontSize: 13,
          color: "#9ca3af",
        }}
      >
        Showing {series.length} series (protocol × server). Scroll to see more.
      </div>

      <div
        style={{
          flex: 1,
          overflowY: "auto",
          display: "flex",
          flexDirection: "column",
          gap: 16,
          paddingRight: 4,
        }}
      >
        {series.map((s) => (
          <div
            key={s.id}
            style={{
              background:
                "linear-gradient(135deg, rgba(15,23,42,0.95), rgba(15,23,42,0.7))",
              borderRadius: 16,
              border: "1px solid rgba(148,163,184,0.3)",
              padding: 16,
              boxShadow: "0 18px 45px rgba(15,23,42,0.9)",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "baseline",
                marginBottom: 8,
              }}
            >
              <div>
                <div
                  style={{
                    fontSize: 15,
                    fontWeight: 600,
                    letterSpacing: 0.3,
                    textTransform: "uppercase",
                  }}
                >
                  {s.protocol} · {s.serverName}
                </div>
                <div
                  style={{
                    fontSize: 12,
                    color: "#9ca3af",
                    marginTop: 2,
                  }}
                >
                  {s.fileName}
                </div>
              </div>
            </div>

            <div
              style={{
                display: "flex",
                flexDirection: "row",
                gap: 16,
                flexWrap: "wrap",
              }}
            >
              <div style={{ flex: 1, minWidth: 260, minHeight: 220 }}>
                <div
                  style={{
                    fontSize: 12,
                    color: "#e5e7eb",
                    marginBottom: 4,
                  }}
                >
                  RTT vs Payload Size
                </div>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={s.points}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                    <XAxis
                      dataKey="payload"
                      stroke="#9ca3af"
                      tick={{ fontSize: 11 }}
                    />
                    <YAxis
                      stroke="#9ca3af"
                      tick={{ fontSize: 11 }}
                      tickFormatter={(v) => `${v.toFixed(2)}`}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#020617",
                        border: "1px solid #4b5563",
                        fontSize: 11,
                      }}
                      labelFormatter={(v) => `Payload: ${v} bytes`}
                    />
                    <Legend wrapperStyle={{ fontSize: 11 }} />
                    <Line
                      type="monotone"
                      dataKey="avgRtt"
                      name="Avg RTT (ms)"
                      stroke="#38bdf8"
                      strokeWidth={2}
                      dot={{ r: 3 }}
                      activeDot={{ r: 5 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>

              <div style={{ flex: 1, minWidth: 260, minHeight: 220 }}>
                <div
                  style={{
                    fontSize: 12,
                    color: "#e5e7eb",
                    marginBottom: 4,
                  }}
                >
                  Throughput vs Payload Size
                </div>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={s.points}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                    <XAxis
                      dataKey="payload"
                      stroke="#9ca3af"
                      tick={{ fontSize: 11 }}
                    />
                    <YAxis
                      stroke="#9ca3af"
                      tick={{ fontSize: 11 }}
                      tickFormatter={(v) => `${v.toFixed(1)}`}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#020617",
                        border: "1px solid #4b5563",
                        fontSize: 11,
                      }}
                      labelFormatter={(v) => `Payload: ${v} bytes`}
                    />
                    <Legend wrapperStyle={{ fontSize: 11 }} />
                    <Line
                      type="monotone"
                      dataKey="throughput"
                      name="Throughput (Mbps)"
                      stroke="#a855f7"
                      strokeWidth={2}
                      dot={{ r: 3 }}
                      activeDot={{ r: 5 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

