import { Button, Container, Stack, Text, Title } from '@mantine/core';
import { Component, type ErrorInfo, type ReactNode } from 'react';

// Sem isso, qualquer erro de runtime não tratado em qualquer componente
// (ex.: acessar propriedade de undefined numa resposta inesperada da API)
// desmonta a árvore inteira do React e deixa uma tela em branco, sem
// nenhuma pista de que algo quebrou — React não tem esse comportamento
// como hook, só via classe (class component é a forma correta aqui).
interface Props {
	children: ReactNode;
}

interface State {
	hasError: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
	state: State = { hasError: false };

	static getDerivedStateFromError(): State {
		return { hasError: true };
	}

	componentDidCatch(error: Error, info: ErrorInfo) {
		console.error('Erro não tratado na interface:', error, info.componentStack);
	}

	render() {
		if (this.state.hasError) {
			return (
				<Container size="sm" py="xl">
					<Stack align="center" gap="sm">
						<Title order={2}>Algo deu errado</Title>
						<Text c="dimmed">
							Ocorreu um erro inesperado na interface. Recarregue a página para continuar.
						</Text>
						<Button onClick={() => window.location.assign('/')}>Recarregar</Button>
					</Stack>
				</Container>
			);
		}

		return this.props.children;
	}
}
