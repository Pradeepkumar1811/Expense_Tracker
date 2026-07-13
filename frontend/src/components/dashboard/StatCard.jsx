import { useMemo } from 'react';

const defaultStyles = {
  card: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '1rem',
    transition: 'box-shadow 0.2s ease',
  },
  iconWrapper: {
    width: '48px',
    height: '48px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1.5rem',
    flexShrink: 0,
  },
  content: {
    flex: 1,
    minWidth: 0,
  },
  title: {
    margin: 0,
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: '0.025em',
  },
  value: {
    margin: '0.25rem 0 0',
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
    lineHeight: 1.2,
  },
};

function StatCard({ title, value, icon, color = '#3b82f6' }) {
  const iconBg = useMemo(
    () => ({
      ...defaultStyles.iconWrapper,
      backgroundColor: `${color}15`,
      color,
    }),
    [color]
  );

  return (
    <div style={defaultStyles.card} role="region" aria-label={title}>
      {icon && <div style={iconBg} aria-hidden="true">{icon}</div>}
      <div style={defaultStyles.content}>
        <p style={defaultStyles.title}>{title}</p>
        <p style={defaultStyles.value}>{value}</p>
      </div>
    </div>
  );
}

export default StatCard;
