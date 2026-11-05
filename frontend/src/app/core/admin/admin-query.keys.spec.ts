import type { QueryClient } from '@tanstack/angular-query-experimental';
import { describe, expect, it, vi } from 'vitest';
import { adminQueryKeys, invalidateAdminAuditLogs } from './admin-query.keys';

describe('adminQueryKeys', () => {
  it('builds user list key with paging and sorting', () => {
    expect(adminQueryKeys.userList(2, 10, 'email,asc')).toEqual([
      'admin',
      'users',
      'list',
      2,
      10,
      'email,asc',
    ]);
  });

  it('exposes stable roles key', () => {
    expect(adminQueryKeys.roles()).toEqual(['admin', 'roles']);
  });

  it('audit log list keys sit under the auditLogs prefix', () => {
    expect(adminQueryKeys.auditLogList(0, 5, 'createdAt,desc').slice(0, 2)).toEqual(adminQueryKeys.auditLogs());
  });

  it('invalidateAdminAuditLogs invalidates every audit log list (dashboard timeline included)', () => {
    const invalidateQueries = vi.fn().mockResolvedValue(undefined);
    invalidateAdminAuditLogs({ invalidateQueries } as unknown as QueryClient);
    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['admin', 'auditLogs'] });
  });
});
