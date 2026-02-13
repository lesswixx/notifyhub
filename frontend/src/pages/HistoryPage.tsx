import { useState, useEffect } from 'react';
import { NotificationDto, notificationApi } from '../api';

export default function HistoryPage() {
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadNotifications();
  }, [statusFilter, page]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationApi.list(page, 50, statusFilter || undefined);
      setNotifications(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (dateStr: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  return (
    <div>
      <div className="page-header">
        <h2>History</h2>
      </div>

      <div className="toolbar">
        <label style={{ fontWeight: 500 }}>Status:</label>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
          <option value="">All</option>
          <option value="CREATED">CREATED</option>
          <option value="QUEUED">QUEUED</option>
          <option value="SENT">SENT</option>
          <option value="FAILED">FAILED</option>
        </select>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.5rem' }}>
          <button className="btn btn-sm btn-primary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
            Prev
          </button>
          <span style={{ alignSelf: 'center', fontSize: '0.875rem', color: '#64748b' }}>Page {page + 1}</span>
          <button className="btn btn-sm btn-primary" disabled={notifications.length < 50} onClick={() => setPage(p => p + 1)}>
            Next
          </button>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading...</div>
      ) : notifications.length === 0 ? (
        <div className="empty-state">
          <h3>No notifications found</h3>
          <p>Notifications will appear here as events are processed.</p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Event</th>
                  <th>Source</th>
                  <th>Channel</th>
                  <th>Status</th>
                  <th>Priority</th>
                  <th>Attempts</th>
                  <th>Error</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {notifications.map((n) => (
                  <tr key={n.id}>
                    <td>{n.id}</td>
                    <td style={{ maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {n.eventTitle || '-'}
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          n.eventSourceType === 'GITHUB' ? 'badge-github' :
                          n.eventSourceType === 'RSS' ? 'badge-rss' : 'badge-gen'
                        }`}
                      >
                        {n.eventSourceType || '-'}
                      </span>
                    </td>
                    <td><span className="badge badge-info">{n.channel}</span></td>
                    <td>
                      <span
                        className={`badge ${
                          n.status === 'SENT' ? 'badge-success' :
                          n.status === 'FAILED' ? 'badge-danger' :
                          n.status === 'QUEUED' ? 'badge-warning' : 'badge-gray'
                        }`}
                      >
                        {n.status}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          n.eventPriority === 'HIGH' ? 'badge-danger' :
                          n.eventPriority === 'MEDIUM' ? 'badge-warning' : 'badge-success'
                        }`}
                      >
                        {n.eventPriority || '-'}
                      </span>
                    </td>
                    <td>{n.attempts}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: '#ef4444' }}>
                      {n.lastError || '-'}
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatTime(n.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
