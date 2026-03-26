import React, { useEffect, useState } from "react";
import { GraphDashboard } from "./GraphDashboard";

export const App: React.FC = () => {
  const [datasets, setDatasets] = useState<any[]>([]);

  useEffect(() => {
    let cancelled = false;

    const fetchDataset = async () => {
      try {
        const res = await fetch(`/plot_results.json?ts=${Date.now()}`);
        if (!res.ok) {
          return;
        }
        const json = await res.json();
        if (!cancelled) {
          if (Array.isArray(json)) {
            setDatasets(json);
          } else {
            setDatasets([json]);
          }
        }
      } catch (_e) {
        // keep last good dataset on fetch errors
      }
    };

    fetchDataset();
    const id = setInterval(fetchDataset, 3000);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#050816",
        color: "#f9fafb",
        display: "flex",
        flexDirection: "column",
      }}
    >

      <main
        style={{
          flex: 1,
          padding: "16px 24px 24px",
          display: "flex",
          flexDirection: "column",
        }}
      >
        <GraphDashboard datasets={datasets} />
      </main>
    </div>
  );
};

