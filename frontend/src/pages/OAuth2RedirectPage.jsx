import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function OAuth2RedirectPage() {
  const { loadUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadUser().then(() => {
      navigate('/dashboard', { replace: true });
    });
  }, [loadUser, navigate]);

  return (
    <div className="flex h-screen items-center justify-center">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="mt-4 text-sm text-muted-foreground">Completing sign in...</p>
      </div>
    </div>
  );
}
