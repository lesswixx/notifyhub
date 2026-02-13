import { useState, useEffect } from 'react';
import { Stats, monitoringApi } from '../api';

export default function MonitoringPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [health, setHealth] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 10000); // refresh every 10s
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    try {
      const [statsData, healthResp] = await Promise.all([
        monitoringApi.stats(),
        fetch('/actuator/health').then((r) => r.json()).catch(() => null),
      ]);
      setStats(statsData);
      setHealth(healthResp);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="empty-state">Loading monitoring data...</div>;
  }

  return (
    <div>
      <div className="page-header">
        <h2>Monitoring</h2>
        <span className="sse-status" style={{ fontSize: '0.8rem', color: '#64748b' }}>
          Auto-refresh: 10s
        </span>
      </div>

      <div className="card-grid" style={{ marginBottom: '1.5rem' }}>
        <div className="stat-card">
          <div className="stat-value">{stats?.totalUsers ?? '-'}</div>
          <div className="stat-label">Total Users</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.totalSubscriptions ?? '-'}</div>
          <div className="stat-label">Subscriptions</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.totalEvents ?? '-'}</div>
          <div className="stat-label">Events Processed</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.totalNotifications ?? '-'}</div>
          <div className="stat-label">Notifications Sent</div>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>System Health</h3>
        {health ? (
          <div>
            <div style={{ marginBottom: '0.75rem' }}>
              <strong>Status: </strong>
              <span className={`badge ${health.status === 'UP' ? 'badge-success' : 'badge-danger'}`}>
                {health.status}
              </span>
            </div>
            {health.components && (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Component</th>
                      <th>Status</th>
                      <th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(health.components).map(([name, data]: [string, any]) => (
                      <tr key={name}>
                        <td>{name}</td>
                        <td>
                          <span className={`badge ${data.status === 'UP' ? 'badge-success' : 'badge-danger'}`}>
                            {data.status}
                          </span>
                        </td>
                        <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                          {data.details ? JSON.stringify(data.details) : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ) : (
          <p style={{ color: '#64748b' }}>Health endpoint unavailable</p>
        )}
      </div>

      <div className="card" style={{ marginTop: '1rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Connectors</h3>
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Source Type</th>
                <th>Status</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><span className="badge badge-github">GITHUB</span></td>
                <td><span className="badge badge-success">Active</span></td>
                <td>GitHub Releases poller (WebClient + Flux.interval)</td>
              </tr>
              <tr>
                <td><span className="badge badge-rss">RSS</span></td>
                <td><span className="badge badge-success">Active</span></td>
                <td>RSS/Atom feed parser (WebClient + ROME)</td>
              </tr>
              <tr>
                <td><span className="badge badge-gen">GEN</span></td>
                <td><span className="badge badge-success">Active</span></td>
                <td>Internal event generator (Flux.generate)</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
