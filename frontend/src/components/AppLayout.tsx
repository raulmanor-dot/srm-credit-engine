import { AppShell, Group, Title, Tabs } from '@mantine/core';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const TABS = [
	{ value: '/', label: 'Painel do Operador' },
	{ value: '/transacoes', label: 'Grid de Transações' },
];

export function AppLayout() {
	const location = useLocation();
	const navigate = useNavigate();
	const activeTab = TABS.some((tab) => tab.value === location.pathname) ? location.pathname : '/';

	return (
		<AppShell header={{ height: 60 }} padding="md">
			<AppShell.Header>
				<Group h="100%" px="md" justify="space-between">
					<Title order={3}>SRM Credit Engine</Title>
					<Tabs value={activeTab} onChange={(value) => value && navigate(value)}>
						<Tabs.List>
							{TABS.map((tab) => (
								<Tabs.Tab key={tab.value} value={tab.value}>
									{tab.label}
								</Tabs.Tab>
							))}
						</Tabs.List>
					</Tabs>
				</Group>
			</AppShell.Header>
			<AppShell.Main>
				<Outlet />
			</AppShell.Main>
		</AppShell>
	);
}
