/** TanStack Query key for the public demo access. Not user-scoped: login/logout keep it. */
export const demoQueryKeys = {
  access: () => ['demo-access'] as const,
};

/**
 * The demo settings only change on a redeploy, so fetch once per page load: no retry (a 404 or a
 * network error just hides the card) and no refetch on focus.
 */
export const DEMO_ACCESS_QUERY_OPTIONS = {
  retry: false,
  staleTime: 60 * 60 * 1000,
  refetchOnWindowFocus: false,
} as const;
