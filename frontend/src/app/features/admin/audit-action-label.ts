import type { TranslationKey } from '../../core/i18n/i18n.service';

/**
 * IAM audit `action` codes written by the backend (see the `IamAuditPort.record` calls in the admin
 * use cases) mapped to readable i18n labels. Shared by the audit-log screen and the dashboard timeline.
 */
const AUDIT_ACTION_LABEL_KEYS: Readonly<Record<string, TranslationKey>> = {
  'iam.user.create': 'auditActionUserCreate',
  'iam.user.set_roles': 'auditActionUserSetRoles',
  'iam.user.enable': 'auditActionUserEnable',
  'iam.user.disable': 'auditActionUserDisable',
};

/** i18n key for a known action code, or `null` for codes the SPA does not know yet. */
export function auditActionLabelKey(action: string): TranslationKey | null {
  return Object.prototype.hasOwnProperty.call(AUDIT_ACTION_LABEL_KEYS, action)
    ? AUDIT_ACTION_LABEL_KEYS[action]
    : null;
}

/** Readable label for an action code; unknown codes fall back to the raw code. */
export function auditActionLabel(action: string, t: (key: TranslationKey) => string): string {
  const key = auditActionLabelKey(action);
  return key ? t(key) : action;
}
