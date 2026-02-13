import { useState, useEffect, useRef } from 'react';
import { NotificationDto, createNotificationStream, notificationApi } from '../api';

export default function DashboardPage() {
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [connected, setConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    // Load recent notifications
    notificationApi.list(0, 50).then(setNotifications).catch(console.error);

    // Connect SSE
    const es = createNotificationStream(
      (n) => {
        setNotifications((prev) => [n, ...prev].slice(0, 100));
      },
      () => setConnected(false)
    );

    es.onopen = () => setConnected(true);
    eventSourceRef.current = es;

    return () => {
      es.close();
    };
  }, []);

  const formatTime = (dateStr: string) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString();
  };

  const getSourceBadge = (source: string) => {
    const cls = source === 'GITHUB' ? 'badge-github' : source === 'RSS' ? 'badge-rss' : 'badge-gen';
    return <span className={`badge ${cls}`}>{source}</span>;
  };

  const getPriorityBadge = (priority: string) => {
    const cls =
      priority === 'HIGH' ? 'badge-danger' : priority === 'MEDIUM' ? 'badge-warning' : 'badge-success';
    return <span className={`badge ${cls}`}>{priority}</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h2>Dashboard</h2>
        <div className="sse-status">
          <span className={`sse-dot ${connected ? '' : 'disconnected'}`} />
          {connected ? 'Live' : 'Disconnected'}
        </div>
      </div>

      <div className="feed-container">
        {notifications.length === 0 ? (
          <div className="empty-state">
            <h3>No notifications yet</h3>
            <p>Create subscriptions to start receiving events in real-time.</p>
          </div>
        ) : (
          notifications.map((n) => (
            <div key={n.id} className={`feed-item priority-${n.eventPriority || 'MEDIUM'}`}>
              <div className="feed-item-header">
                <span className="feed-item-title">{n.eventTitle || 'Untitled Event'}</span>
                <div className="feed-item-meta">
                  {n.eventSourceType && getSourceBadge(n.eventSourceType)}
                  {n.eventPriority && getPriorityBadge(n.eventPriority)}
                </div>
              </div>
              <div className="feed-item-time">
                {formatTime(n.createdAt)} &middot; {n.channel} &middot;{' '}
                <span className={`badge badge-${n.status === 'SENT' ? 'success' : n.status === 'FAILED' ? 'danger' : 'gray'}`}>
                  {n.status}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
