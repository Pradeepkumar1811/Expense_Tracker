import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './NavigationLayout.css';

function NavigationLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="layout">
      <header className="layout-header">
        <div className="header-content">
          <button
            className="menu-toggle"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={menuOpen}
          >
            <span className="menu-icon">{menuOpen ? '✕' : '☰'}</span>
          </button>
          <h1 className="app-title">Expense Tracker</h1>
          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <div className="layout-body">
        <nav className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`} aria-label="Main navigation">
          <ul className="nav-list">
            <li>
              <NavLink to="/" end className="nav-link" onClick={closeMenu}>
                Dashboard
              </NavLink>
            </li>
            <li>
              <NavLink to="/transactions" className="nav-link" onClick={closeMenu}>
                Transactions
              </NavLink>
            </li>
            <li>
              <NavLink to="/categories" className="nav-link" onClick={closeMenu}>
                Categories
              </NavLink>
            </li>
            <li>
              <NavLink to="/budgets" className="nav-link" onClick={closeMenu}>
                Budgets
              </NavLink>
            </li>
            <li>
              <NavLink to="/subscriptions" className="nav-link" onClick={closeMenu}>
                Subscriptions
              </NavLink>
            </li>
            <li>
              <NavLink to="/reminders" className="nav-link" onClick={closeMenu}>
                Reminders
              </NavLink>
            </li>
            <li>
              <NavLink to="/reports" className="nav-link" onClick={closeMenu}>
                Reports
              </NavLink>
            </li>
          </ul>
        </nav>

        {menuOpen && (
          <div
            className="sidebar-overlay"
            onClick={closeMenu}
            aria-hidden="true"
          />
        )}

        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default NavigationLayout;
