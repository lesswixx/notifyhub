import { useState, useEffect } from 'react';
import { Rule, Subscription, ruleApi, subscriptionApi } from '../api';

export default function RulesPage() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [selectedSubId, setSelectedSubId] = useState<number | null>(null);
  const [rules, setRules] = useState<Rule[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editItem, setEditItem] = useState<Rule | null>(null);
  const [form, setForm] = useState({
    keywordFilter: '',
    dedupWindowMinutes: 0,
    rateLimitPerHour: 0,
    priority: 'MEDIUM',
    quietHoursStart: '',
    quietHoursEnd: '',
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    subscriptionApi.list().then((subs) => {
      setSubscriptions(subs);
      if (subs.length > 0) {
        setSelectedSubId(subs[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (selectedSubId) {
      ruleApi.list(selectedSubId).then(setRules).catch(console.error);
    }
  }, [selectedSubId]);

  const openCreate = () => {
    if (!selectedSubId) return;
    setEditItem(null);
    setForm({
      keywordFilter: '',
      dedupWindowMinutes: 0,
      rateLimitPerHour: 0,
      priority: 'MEDIUM',
      quietHoursStart: '',
      quietHoursEnd: '',
    });
    setShowModal(true);
  };

  const openEdit = (rule: Rule) => {
    setEditItem(rule);
    setForm({
      keywordFilter: rule.keywordFilter || '',
      dedupWindowMinutes: rule.dedupWindowMinutes,
      rateLimitPerHour: rule.rateLimitPerHour,
      priority: rule.priority,
      quietHoursStart: rule.quietHoursStart || '',
      quietHoursEnd: rule.quietHoursEnd || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async () => {
    if (!selectedSubId) return;
    setLoading(true);
    try {
      const data: Partial<Rule> = {
        subscriptionId: selectedSubId,
        keywordFilter: form.keywordFilter || undefined,
        dedupWindowMinutes: form.dedupWindowMinutes,
        rateLimitPerHour: form.rateLimitPerHour,
        priority: form.priority,
        quietHoursStart: form.quietHoursStart || undefined,
        quietHoursEnd: form.quietHoursEnd || undefined,
      };

      if (editItem) {
        await ruleApi.update(editItem.id, data);
      } else {
        await ruleApi.create(data);
      }
      setShowModal(false);
      ruleApi.list(selectedSubId).then(setRules);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this rule?')) return;
    await ruleApi.delete(id);
    if (selectedSubId) ruleApi.list(selectedSubId).then(setRules);
  };

  return (
    <div>
      <div className="page-header">
        <h2>Rules</h2>
        <button className="btn btn-primary" onClick={openCreate} disabled={!selectedSubId}>
          + New Rule
        </button>
      </div>

      <div className="toolbar">
        <label style={{ fontWeight: 500 }}>Subscription:</label>
        <select
          value={selectedSubId || ''}
          onChange={(e) => setSelectedSubId(Number(e.target.value))}
        >
          {subscriptions.map((sub) => (
            <option key={sub.id} value={sub.id}>
              #{sub.id} - {sub.sourceType} ({sub.params})
            </option>
          ))}
        </select>
      </div>

      {!selectedSubId ? (
        <div className="empty-state">
          <h3>No subscriptions</h3>
          <p>Create a subscription first, then add rules to it.</p>
        </div>
      ) : rules.length === 0 ? (
        <div className="empty-state">
          <h3>No rules for this subscription</h3>
          <p>All events from this source will generate notifications without filtering.</p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Keywords</th>
                  <th>Dedup (min)</th>
                  <th>Rate/hr</th>
                  <th>Priority</th>
                  <th>Quiet Hours</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id}>
                    <td>{rule.id}</td>
                    <td>{rule.keywordFilter || <span style={{ color: '#94a3b8' }}>any</span>}</td>
                    <td>{rule.dedupWindowMinutes || '-'}</td>
                    <td>{rule.rateLimitPerHour || '-'}</td>
                    <td>
                      <span
                        className={`badge ${
                          rule.priority === 'HIGH' ? 'badge-danger' : rule.priority === 'MEDIUM' ? 'badge-warning' : 'badge-success'
                        }`}
                      >
                        {rule.priority}
                      </span>
                    </td>
                    <td>
                      {rule.quietHoursStart && rule.quietHoursEnd
                        ? `${rule.quietHoursStart} - ${rule.quietHoursEnd}`
                        : '-'}
                    </td>
                    <td>
                      <button className="btn btn-sm btn-primary" onClick={() => openEdit(rule)} style={{ marginRight: '0.5rem' }}>
                        Edit
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => handleDelete(rule.id)}>
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
            <h3>{editItem ? 'Edit Rule' : 'New Rule'}</h3>

            <div className="form-group">
              <label>Keyword Filter (comma-separated)</label>
              <input
                type="text"
                value={form.keywordFilter}
                onChange={(e) => setForm({ ...form, keywordFilter: e.target.value })}
                placeholder="e.g. security, update, critical"
              />
            </div>

            <div className="form-group">
              <label>Dedup Window (minutes)</label>
              <input
                type="number"
                min={0}
                value={form.dedupWindowMinutes}
                onChange={(e) => setForm({ ...form, dedupWindowMinutes: parseInt(e.target.value) || 0 })}
              />
            </div>

            <div className="form-group">
              <label>Rate Limit (per hour, 0 = unlimited)</label>
              <input
                type="number"
                min={0}
                value={form.rateLimitPerHour}
                onChange={(e) => setForm({ ...form, rateLimitPerHour: parseInt(e.target.value) || 0 })}
              />
            </div>

            <div className="form-group">
              <label>Priority</label>
              <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div className="form-group">
              <label>Quiet Hours Start</label>
              <input
                type="time"
                value={form.quietHoursStart}
                onChange={(e) => setForm({ ...form, quietHoursStart: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label>Quiet Hours End</label>
              <input
                type="time"
                value={form.quietHoursEnd}
                onChange={(e) => setForm({ ...form, quietHoursEnd: e.target.value })}
              />
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
