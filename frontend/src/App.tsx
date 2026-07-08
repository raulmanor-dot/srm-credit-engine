import { Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { NotFoundPage } from './pages/NotFoundPage';
import { OperatorPanelPage } from './pages/OperatorPanelPage';
import { TransactionsGridPage } from './pages/TransactionsGridPage';

export default function App() {
	return (
		<Routes>
			<Route element={<AppLayout />}>
				<Route path="/" element={<OperatorPanelPage />} />
				<Route path="/transacoes" element={<TransactionsGridPage />} />
				<Route path="*" element={<NotFoundPage />} />
			</Route>
		</Routes>
	);
}
