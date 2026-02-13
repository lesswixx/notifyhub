import { useState, useEffect } from 'react';
import { Subscription, subscriptionApi } from '../api';

export default function SubscriptionsPage() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editItem, setEditItem] = useState<Subscription | null>(null);
  const [form, setForm] = useState({ sourceType: 'GITHUB', params: '', enabled: true });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadSubscriptions();
  }, []);

  const loadSubscriptions = () => {
    subscriptionApi.list().then(setSubscriptions).catch(console.error);
  };

  const openCreate = () => {
    setEditItem(null);
    setForm({ sourceType: 'GITHUB', params: '', enabled: true });
    setShowModal(true);
  };

  const openEdit = (sub: Subscription) => {
    setEditItem(sub);
    setForm({ sourceType: sub.sourceType, params: sub.params || '', enabled: sub.enabled });
    setShowModal(true);
  };

  const handleSubmit = async () => {
    setLoading(true);
    try {
      if (editItem) {
        await subscriptionApi.update(editItem.id, form);
      } else {
        await subscriptionApi.create(form);
      }
      setShowModal(false);
      loadSubscriptions();
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this subscription?')) return;
    await subscriptionApi.delete(id);
    loadSubscriptions();
  };

  const getParamsHint = (sourceType: string) => {
    switch (sourceType) {
      case 'GITHUB':
        return '{"repo":"owner/repo-name"}';
      case 'RSS':
        return '{"url":"https://example.com/feed.xml"}';
      case 'GEN':
        return '{}';
      default:
        return '{}';
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Subscriptions</h2>
        <button className="btn btn-primary" onClick={openCreate}>
          + New Subscription
        </button>
      </div>

      {subscriptions.length === 0 ? (
        <div className="empty-state">
          <h3>No subscriptions yet</h3>
          <p>Add your first subscription to start receiving notifications.</p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Source</th>
                  <th>Parameters</th>
                  <th>Enabled</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {subscriptions.map((sub) => (
                  <tr key={sub.id}>
                    <td>{sub.id}</td>
                    <td>
                      <span
                        className={`badge ${
                          sub.sourceType === 'GITHUB'
                            ? 'badge-github'
                            : sub.sourceType === 'RSS'
                            ? 'badge-rss'
                            : 'badge-gen'
                        }`}
                      >
                        {sub.sourceType}
                      </span>
                    </td>
                    <td>
                      <code style={{ fontSize: '0.8rem' }}>{sub.params}</code>
                    </td>
                    <td>
                      <span className={`badge ${sub.enabled ? 'badge-success' : 'badge-gray'}`}>
                        {sub.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td>{sub.createdAt ? new Date(sub.createdAt).toLocaleDateString() : '-'}</td>
                    <td>
                      <button className="btn btn-sm btn-primary" onClick={() => openEdit(sub)} style={{ marginRight: '0.5rem' }}>
                        Edit
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => handleDelete(sub.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editItem ? 'Edit Subscription' : 'New Subscription'}</h3>

            <div className="form-group">
              <label>Source Type</label>
              <select
                value={form.sourceType}
                onChange={(e) =>
                  setForm({ ...form, sourceType: e.target.value, params: getParamsHint(e.target.value) })
                }
              >
                <option value="GITHUB">GitHub Releases</option>
                <option value="RSS">RSS / Atom Feed</option>
                <option value="GEN">Event Generator (Demo)</option>
              </select>
            </div>

            <div className="form-group">
              <label>Parameters (JSON)</label>
              <textarea
                rows={3}
                value={form.params}
                onChange={(e) => setForm({ ...form, params: e.target.value })}
                placeholder={getParamsHint(form.sourceType)}
              />
            </div>

            <div className="form-group">
              <label>
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
                  style={{ marginRight: '0.5rem' }}
                />
                Enabled
              </label>
            </div>

            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowModal(false)} style={{ color: '#64748b', borderColor: '#e2e8f0' }}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={loading}>
                {loading ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
