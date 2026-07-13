import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { LoginForm, RegisterForm, ProtectedRoute } from './components/auth';
import { NavigationLayout } from './components/layout';
import { DashboardView } from './components/dashboard';
import { TransactionList } from './components/transactions';
import { CategoryList } from './components/categories';
import { BudgetList } from './components/budgets';
import { SubscriptionList } from './components/subscriptions';
import { ReminderList } from './components/reminders';
import { ReportView } from './components/reports';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginForm />} />
          <Route path="/register" element={<RegisterForm />} />

          {/* Protected routes with navigation layout */}
          <Route
            element={
              <ProtectedRoute>
                <NavigationLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardView />} />
            <Route path="/transactions" element={<TransactionList />} />
            <Route path="/categories" element={<CategoryList />} />
            <Route path="/budgets" element={<BudgetList />} />
            <Route path="/subscriptions" element={<SubscriptionList />} />
            <Route path="/reminders" element={<ReminderList />} />
            <Route path="/reports" element={<ReportView />} />
          </Route>

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
