const domain = import.meta.env.VITE_COGNITO_DOMAIN ?? '';
const clientId = import.meta.env.VITE_COGNITO_CLIENT_ID ?? '';
const redirectUri = import.meta.env.VITE_COGNITO_REDIRECT_URI ?? window.location.origin;
const logoutUri = import.meta.env.VITE_COGNITO_LOGOUT_URI ?? window.location.origin;
const scope = import.meta.env.VITE_COGNITO_SCOPE ?? 'openid profile email';

export const cognitoConfig = {
  domain,
  clientId,
  redirectUri,
  logoutUri,
  scope
};

export function buildLoginUrl(): string {
  const params = new URLSearchParams({
    client_id: cognitoConfig.clientId,
    response_type: 'token',
    scope: cognitoConfig.scope,
    redirect_uri: cognitoConfig.redirectUri
  });
  return `https://${cognitoConfig.domain}/login?${params.toString()}`;
}

export function buildLogoutUrl(): string {
  const params = new URLSearchParams({
    client_id: cognitoConfig.clientId,
    logout_uri: cognitoConfig.logoutUri
  });
  return `https://${cognitoConfig.domain}/logout?${params.toString()}`;
}

export function parseTokenFromHash(): string | null {
  if (!window.location.hash) {
    return null;
  }

  const hash = new URLSearchParams(window.location.hash.slice(1));
  return hash.get('id_token');
}

export function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}
