import type { components } from './openapi.generated';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}

type OpenApiBranch = components['schemas']['BranchResponse'];
export type Branch = {
  id: NonNullable<OpenApiBranch['id']>;
  code: NonNullable<OpenApiBranch['code']>;
  name: NonNullable<OpenApiBranch['name']>;
  city: NonNullable<OpenApiBranch['city']>;
  isActive: NonNullable<OpenApiBranch['isActive']>;
  createdAt: NonNullable<OpenApiBranch['createdAt']>;
  updatedAt: NonNullable<OpenApiBranch['updatedAt']>;
};

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}

export type CreateBranchPayload = components['schemas']['CreateBranchRequest'];

export type UpdateBranchPayload = components['schemas']['UpdateBranchRequest'];

type OpenApiLoginResponse = components['schemas']['LoginResponse'];
export type LoginResponse = {
  token?: OpenApiLoginResponse['token'];
  mfaChallengeToken?: OpenApiLoginResponse['mfaChallengeToken'];
};

export type LoginRequest = components['schemas']['LoginRequest'];

type OpenApiMe = components['schemas']['MeResponse'];
export type MeProfile = {
  id: NonNullable<OpenApiMe['id']>;
  email: NonNullable<OpenApiMe['email']>;
  enabled: NonNullable<OpenApiMe['enabled']>;
  lockedUntil: OpenApiMe['lockedUntil'] | null | undefined;
  createdAt: NonNullable<OpenApiMe['createdAt']>;
  roles: NonNullable<OpenApiMe['roles']>;
  mfaEnabled: NonNullable<OpenApiMe['mfaEnabled']>;
};

type OpenApiAdminUser = components['schemas']['AdminUserResponse'];
export type AdminUser = {
  id: NonNullable<OpenApiAdminUser['id']>;
  email: NonNullable<OpenApiAdminUser['email']>;
  enabled: NonNullable<OpenApiAdminUser['enabled']>;
  lockedUntil: OpenApiAdminUser['lockedUntil'] | null | undefined;
  createdAt: NonNullable<OpenApiAdminUser['createdAt']>;
  roles: NonNullable<OpenApiAdminUser['roles']>;
  mfaEnabled: NonNullable<OpenApiAdminUser['mfaEnabled']>;
};

type OpenApiAdminRole = components['schemas']['AdminRoleResponse'];
export type AdminRole = {
  id: NonNullable<OpenApiAdminRole['id']>;
  name: NonNullable<OpenApiAdminRole['name']>;
};

export type CreateUserPayload = components['schemas']['CreateUserRequest'];
export type SetUserRolesPayload = components['schemas']['SetUserRolesRequest'];
export type SetUserEnabledPayload = components['schemas']['SetUserEnabledRequest'];

type OpenApiAuditLog = components['schemas']['AdminAuditLogResponse'];
export type AuditLogEntry = {
  id: NonNullable<OpenApiAuditLog['id']>;
  actorUserId: NonNullable<OpenApiAuditLog['actorUserId']>;
  action: NonNullable<OpenApiAuditLog['action']>;
  targetUserId: OpenApiAuditLog['targetUserId'] | null | undefined;
  metadata: OpenApiAuditLog['metadata'] | null | undefined;
  createdAt: NonNullable<OpenApiAuditLog['createdAt']>;
};

/** Align with `RefreshSessionResponse` (IAM3). */
export type RefreshSessionRow = {
  id: string;
  familyId: string;
  createdAt: string;
  expiresAt: string;
  current: boolean;
};

export type MfaEnrollBeginResponse = {
  otpauthUri: string;
  secretBase32: string;
};

export type RevokeOthersResponse = {
  revokedCount: number;
};
