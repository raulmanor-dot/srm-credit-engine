import { Button, Container, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router-dom';

// Sem esta rota, um path desconhecido (typo, link antigo, refresh numa rota
// removida) renderizava uma página inteiramente em branco — nem o header
// aparecia, porque nenhuma <Route> casava e o React Router não renderiza nada.
export function NotFoundPage() {
	return (
		<Container size="sm" py="xl">
			<Stack align="center" gap="sm">
				<Title order={2}>Página não encontrada</Title>
				<Text c="dimmed">O endereço acessado não existe nesta aplicação.</Text>
				<Button component={Link} to="/">
					Voltar ao Painel do Operador
				</Button>
			</Stack>
		</Container>
	);
}
