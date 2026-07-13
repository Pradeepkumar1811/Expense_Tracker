import { useState, useEffect } from 'react';
import reminderService from '../../services/reminderService';
import ReminderItem from './ReminderItem';

const styles = {
  container: {
    maxWidth: '800px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    margin: '0 0 1.5rem',
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '200px',
    color: '#6b7280',
    fontSize: '1rem',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '200px',
    gap: '1rem',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '1rem',
    margin: 0,
  },
  retryButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  emptyText: {
    color: '#6b7280',
    fontSize: '0.95rem',
    textAlign: 'center',
    padding: '2rem 0',
  },
};

function ReminderList() {
  const [reminders, setReminders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReminders = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await reminderService.getAll();
      setReminders(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load reminders.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReminders();
  }, []);

  const handleMarkAsRead = async (id) => {
    try {
      await reminderService.markAsRead(id);
      setReminders((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to mark reminder as read.');
    }
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading reminders...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchReminders}>Retry</button>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <h2 style={styles.header}>Reminders</h2>

      {reminders.length === 0 ? (
        <p style={styles.emptyText}>No upcoming reminders. You're all caught up!</p>
      ) : (
        <div style={styles.list} role="list" aria-label="Reminder list">
          {reminders.map((reminder) => (
            <ReminderItem
              key={reminder.id}
              reminder={reminder}
              onMarkAsRead={handleMarkAsRead}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default ReminderList;
