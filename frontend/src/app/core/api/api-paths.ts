import type { paths } from '../models/openapi.generated';

/** String must match a key of the generated OpenAPI `paths` map. */
function apiPath<P extends keyof paths & string>(path: P): P {
  return path;
}

/** Static route prefixes from the OpenAPI contract (`openapi.generated.ts`). */
export const ApiPaths = {
  authLogin: apiPath('/api/v1/auth/login'),
  authLogout: apiPath('/api/v1/auth/logout'),
  authRefresh: apiPath('/api/v1/auth/refresh'),
  me: apiPath('/api/v1/me'),
  publicDemo: apiPath('/api/v1/public/demo'),
  branches: apiPath('/api/v1/branches'),
  adminRoles: apiPath('/api/v1/admin/roles'),
  adminUsers: apiPath('/api/v1/admin/users'),
  adminAuditLogs: apiPath('/api/v1/admin/audit-logs'),
} as const;

/** Resolves `GET|PUT /api/v1/branches/{id}` — must stay aligned with OpenAPI `paths`. */
export function branchByIdPath(id: string): string {
  return `${ApiPaths.branches}/${encodeURIComponent(id)}`;
}

/** Resolves `PATCH /api/v1/branches/{id}/deactivate`. */
export function branchDeactivatePath(id: string): string {
  return `${ApiPaths.branches}/${encodeURIComponent(id)}/deactivate`;
}
