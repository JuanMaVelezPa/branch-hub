import { describe, expect, it } from 'vitest';
import type { TranslationKey } from '../../core/i18n/i18n.service';
import { auditActionLabel, auditActionLabelKey } from './audit-action-label';

describe('audit action labels', () => {
  const t = (key: TranslationKey) => `t:${key}`;

  it.each([
    ['iam.user.create', 'auditActionUserCreate'],
    ['iam.user.set_roles', 'auditActionUserSetRoles'],
    ['iam.user.enable', 'auditActionUserEnable'],
    ['iam.user.disable', 'auditActionUserDisable'],
  ])('maps %s to %s', (action, key) => {
    expect(auditActionLabelKey(action)).toBe(key);
    expect(auditActionLabel(action, t)).toBe(`t:${key}`);
  });

  it('falls back to the raw code for unknown actions', () => {
    expect(auditActionLabelKey('iam.session.revoke')).toBeNull();
    expect(auditActionLabel('iam.session.revoke', t)).toBe('iam.session.revoke');
  });

  it('does not treat inherited object properties as known codes', () => {
    expect(auditActionLabelKey('toString')).toBeNull();
    expect(auditActionLabel('constructor', t)).toBe('constructor');
  });
});
