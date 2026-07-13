import { useState } from 'react';
import reportService from '../../services/reportService';

const styles = {
  wrapper: {
    position: 'relative',
  },
  exportButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#374151',
    backgroundColor: '#f3f4f6',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '0.4rem',
  },
  dropdown: {
    position: 'absolute',
    top: 'calc(100% + 0.5rem)',
    right: 0,
    background: '#ffffff',
    borderRadius: '12px',
    boxShadow: '0 10px 40px rgba(0, 0, 0, 0.12)',
    padding: '1.25rem',
    zIndex: 100,
    minWidth: '280px',
  },
  dropdownTitle: {
    margin: '0 0 1rem',
    fontSize: '0.95rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.35rem',
    marginBottom: '0.75rem',
  },
  label: {
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#374151',
  },
  input: {
    padding: '0.5rem 0.6rem',
    fontSize: '0.85rem',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    outline: 'none',
    color: '#1f2937',
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.5rem',
    marginTop: '0.75rem',
  },
  cancelButton: {
    padding: '0.4rem 0.75rem',
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#374151',
    backgroundColor: '#f3f4f6',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
  },
  downloadButton: {
    padding: '0.4rem 0.75rem',
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#10b981',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
  },
  downloadButtonDisabled: {
    padding: '0.4rem 0.75rem',
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#6ee7b7',
    border: 'none',
    borderRadius: '6px',
    cursor: 'not-allowed',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '0.8rem',
    margin: '0.5rem 0 0',
  },
};

function getDefaultDateRange() {
  const now = new Date();
  const startDate = new Date(now.getFullYear(), now.getMonth(), 1)
    .toISOString()
    .split('T')[0];
  const endDate = now.toISOString().split('T')[0];
  return { startDate, endDate };
}

function ExportButton() {
  const [open, setOpen] = useState(false);
  const defaults = getDefaultDateRange();
  const [startDate, setStartDate] = useState(defaults.startDate);
  const [endDate, setEndDate] = useState(defaults.endDate);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState(null);

  const handleToggle = () => {
    setOpen((prev) => !prev);
    setError(null);
  };

  const handleClose = () => {
    setOpen(false);
    setError(null);
  };

  const handleDownload = async () => {
    if (!startDate || !endDate) {
      setError('Please select both start and end dates.');
      return;
    }
    if (startDate > endDate) {
      setError('Start date must be before end date.');
      return;
    }

    setError(null);
    setDownloading(true);
    try {
      const blob = await reportService.exportCsv(startDate, endDate);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `transactions_export_${startDate}_${endDate}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      handleClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to export CSV.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div style={styles.wrapper}>
      <button style={styles.exportButton} onClick={handleToggle} aria-label="Export transactions to CSV">
        📥 Export CSV
      </button>

      {open && (
        <div style={styles.dropdown}>
          <p style={styles.dropdownTitle}>Export Date Range</p>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="export-start">Start Date</label>
            <input
              id="export-start"
              type="date"
              style={styles.input}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="export-end">End Date</label>
            <input
              id="export-end"
              type="date"
              style={styles.input}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>

          {error && <p style={styles.errorText} role="alert">{error}</p>}

          <div style={styles.actions}>
            <button style={styles.cancelButton} onClick={handleClose}>
              Cancel
            </button>
            <button
              style={downloading ? styles.downloadButtonDisabled : styles.downloadButton}
              onClick={handleDownload}
              disabled={downloading}
            >
              {downloading ? 'Downloading...' : 'Download'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default ExportButton;
