import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'
import { storeService, type WhatsAppSettings } from '../services/storeService'
import { whatsappService, type WhatsAppLinkStatus } from '../services/whatsappService'
import { useStoreColor } from '../hooks/useStoreColor'
import { MessageCircle, Loader2, Info, Smartphone, CheckCircle } from 'lucide-react'

const PLACEHOLDERS = [
  '{{customerName}}',
  '{{orderId}}',
  '{{trackingNumber}}',
  '{{totalAmount}}',
  '{{currency}}',
  '{{city}}',
]

const DEFAULT_CONFIRMED = `Bonjour {{customerName}},

Votre commande #{{orderId}} est confirmée.
Elle vous sera livrée sous 24h à 48h au plus tard.
Montant de la commande : {{totalAmount}} {{currency}}.

مرحبا {{customerName}}،
تم تأكيد طلبكم رقم {{orderId}}.
سيتم توصيله خلال 24 إلى 48 ساعة كحد أقصى.
مبلغ الطلب هو {{totalAmount}} {{currency}}.

Merci pour votre confiance !`

const DEFAULT_DELIVERED = `Bonjour {{customerName}},

Votre commande #{{orderId}} a été livrée.
Numéro de suivi : {{trackingNumber}}.

مرحبا {{customerName}}،
تم توصيل طلبكم رقم {{orderId}}.
رقم التتبع: {{trackingNumber}}.

Merci !`

export default function Automations() {
  const { user } = useAuth()
  const { theme } = useTheme()
  const { storeColor } = useStoreColor()
  const isDark = theme === 'dark'
  const textPrimary = isDark ? '#F9FAFB' : '#111827'
  const textSecondary = isDark ? '#9CA3AF' : '#6B7280'
  const panelBg = isDark ? '#2A2D35' : '#FFFFFF'
  const panelBorder = isDark ? '#3d4048' : '#E5E7EB'
  const insetBg = isDark ? '#222328' : '#F9FAFB'
  const inputBg = isDark ? '#222328' : '#FFFFFF'
  const storeId = user?.selectedStoreId
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [settings, setSettings] = useState<WhatsAppSettings | null>(null)
  const [linkStatus, setLinkStatus] = useState<WhatsAppLinkStatus | null>(null)

  useEffect(() => {
    if (!storeId) {
      setLoading(false)
      return
    }
    setError(null)
    storeService
      .getWhatsAppSettings(storeId)
      .then((data) =>
        setSettings({
          ...data,
          whatsappTemplateConfirmed: data.whatsappTemplateConfirmed?.trim() || DEFAULT_CONFIRMED,
          whatsappTemplateDelivered: data.whatsappTemplateDelivered?.trim() || DEFAULT_DELIVERED,
        })
      )
      .catch((e) => setError(e.response?.data?.error || e.message || 'Failed to load settings'))
      .finally(() => setLoading(false))
  }, [storeId])

  // Keep link status fresh at all times (including after linked),
  // so logout/relink is reflected immediately without manual refresh.
  // Link status is now scoped per store so each store can have its own WhatsApp account.
  useEffect(() => {
    if (!storeId) {
      setLinkStatus(null)
      return
    }
    let mounted = true
    const refresh = () => {
      whatsappService
        .getStoreLinkStatus(storeId)
        .then((status) => {
          if (mounted) setLinkStatus(status)
        })
        .catch(() => {
          if (mounted) setLinkStatus(null)
        })
    }
    refresh()
    const t = setInterval(refresh, 2500)
    return () => {
      mounted = false
      clearInterval(t)
    }
  }, [storeId])

  const handleSave = async () => {
    if (!storeId || !settings) return
    setSaving(true)
    setError(null)
    setSuccess(false)
    try {
      const updated = await storeService.updateWhatsAppSettings(storeId, {
        whatsappAutomationEnabled: settings.whatsappAutomationEnabled,
        whatsappTemplateConfirmed: settings.whatsappTemplateConfirmed || '',
        whatsappTemplateDelivered: settings.whatsappTemplateDelivered || '',
      })
      setSettings(updated)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (e: any) {
      setError(e.response?.data?.error || e.message || 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  if (!storeId) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold" style={{ color: textPrimary }}>WhatsApp automations</h1>
        <div className="rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20 p-4 text-amber-800 dark:text-amber-200">
          <p className="text-sm">Please select a store from the header to configure WhatsApp automations.</p>
        </div>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <Loader2 className="h-10 w-10 animate-spin text-gray-400" style={{ color: storeColor }} />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold" style={{ color: textPrimary }}>WhatsApp automations</h1>
        <p className="mt-1 text-sm" style={{ color: textSecondary }}>
          Send automatic WhatsApp messages to customers when order status changes.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4 text-red-700 dark:text-red-300 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div className="rounded-xl border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 p-4 text-green-700 dark:text-green-300 text-sm">
          Settings saved.
        </div>
      )}

      <div
        className="rounded-xl border p-6 shadow-sm"
        style={{ backgroundColor: panelBg, borderColor: panelBorder }}
      >
        <div className="flex items-center gap-2 mb-6">
          <MessageCircle className="h-6 w-6" style={{ color: storeColor }} />
          <h2 className="text-lg font-semibold" style={{ color: textPrimary }}>WhatsApp messages</h2>
        </div>

        {settings?.sendingConfigured === false && (
          <div className="mb-4 p-4 rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20">
            <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
              WhatsApp is not available yet
            </p>
            <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
              Your organization has not enabled WhatsApp. Contact your administrator to turn it on. Once it is enabled, you will be able to link your number here and send automatic messages to your customers.
            </p>
          </div>
        )}

        {linkStatus?.provider === 'bridge' && (
          <div
            className="mb-6 p-6 rounded-xl border"
            style={{ backgroundColor: insetBg, borderColor: panelBorder }}
          >
            <h3 className="text-sm font-semibold flex items-center gap-2 mb-2" style={{ color: textPrimary }}>
              <Smartphone className="h-4 w-4" style={{ color: storeColor }} />
              Link your WhatsApp
            </h3>
            <p className="text-sm mb-4" style={{ color: textSecondary }}>
              Link your WhatsApp number to send automatic order updates and promotions to your customers. Each admin can link their own number.
            </p>
            {linkStatus.ready ? (
              <p className="text-sm text-green-600 dark:text-green-400 flex items-center gap-2">
                <CheckCircle className="h-4 w-4" />
                Your WhatsApp is linked. You can send automations and promotions to your customers.
              </p>
            ) : linkStatus.qr ? (
              <>
                {linkStatus.reason === 'logged_out' && (
                  <div className="mb-4 p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                    <p className="text-sm font-medium text-amber-800 dark:text-amber-200">You were logged out from WhatsApp</p>
                    <p className="text-xs text-amber-700 dark:text-amber-300 mt-0.5">Scan the new QR code below to link your number again.</p>
                  </div>
                )}
                <p className="text-sm mb-3 font-medium" style={{ color: textPrimary }}>
                  Scan with your phone
                </p>
                <p className="text-sm mb-4" style={{ color: textSecondary }}>
                  Open <strong>WhatsApp</strong> on your phone → <strong>Settings</strong> → <strong>Linked devices</strong> → <strong>Link a device</strong>, then scan the QR code below.
                </p>
                <div
                  className="inline-block p-3 rounded-xl border"
                  style={{ backgroundColor: inputBg, borderColor: panelBorder }}
                >
                  <img src={linkStatus.qr} alt="Scan this QR code with WhatsApp on your phone" className="w-[280px] h-[280px]" />
                </div>
              </>
            ) : linkStatus.error || linkStatus.bridgeError ? (
              <div className="rounded-lg border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20 p-4">
                <p className="text-sm font-medium text-amber-800 dark:text-amber-200">Connection problem</p>
                <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
                  {linkStatus.error || linkStatus.bridgeError}
                </p>
                <p className="text-xs text-amber-600 dark:text-amber-400 mt-2">
                  Refresh the page to try again, or contact your administrator.
                </p>
              </div>
            ) : linkStatus.initializing ? (
              <div className="flex items-center gap-3" style={{ color: textSecondary }}>
                <Loader2 className="h-5 w-5 animate-spin shrink-0" style={{ color: storeColor }} />
                <div>
                  {linkStatus.reason === 'logged_out' ? (
                    <>
                      <p className="text-sm font-medium">You were logged out. Preparing a new QR code…</p>
                      <p className="text-xs mt-0.5">A new QR will appear here in a few seconds. Keep this page open.</p>
                    </>
                  ) : (
                    <>
                      <p className="text-sm font-medium">Starting WhatsApp connection…</p>
                      <p className="text-xs mt-0.5">This can take up to a minute. Please wait and keep this page open.</p>
                    </>
                  )}
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-3" style={{ color: textSecondary }}>
                <Loader2 className="h-5 w-5 animate-spin shrink-0" style={{ color: storeColor }} />
                <div>
                  {linkStatus.reason === 'logged_out' ? (
                    <>
                      <p className="text-sm font-medium">You were logged out. Preparing a new QR code…</p>
                      <p className="text-xs mt-0.5">A new QR will appear here in a few seconds.</p>
                    </>
                  ) : (
                    <>
                      <p className="text-sm font-medium">Preparing your QR code…</p>
                      <p className="text-xs mt-0.5">If you logged out from WhatsApp on your phone, a new QR will appear here shortly. Otherwise wait up to a minute or refresh the page.</p>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        <p className="text-sm mb-4" style={{ color: textSecondary }}>
          When an order is set to <strong>Confirmed</strong> or <strong>Delivered</strong>, the message is sent to the customer&apos;s phone number (if WhatsApp is configured).
        </p>

        <div className="flex items-center gap-3 mb-6">
          <button
            type="button"
            role="switch"
            aria-checked={settings?.whatsappAutomationEnabled ?? false}
            onClick={() => setSettings((s) => s ? { ...s, whatsappAutomationEnabled: !s.whatsappAutomationEnabled } : null)}
            className={`
              relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 transition-colors
              ${settings?.whatsappAutomationEnabled ? '' : 'border-gray-200 dark:border-gray-600'}
            `}
            style={
              settings?.whatsappAutomationEnabled
                ? { borderColor: storeColor, backgroundColor: storeColor }
                : { backgroundColor: 'transparent' }
            }
          >
            <span
              className={`
                pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition
                ${settings?.whatsappAutomationEnabled ? 'translate-x-5' : 'translate-x-1'}
              `}
            />
          </button>
          <span className="text-sm font-medium" style={{ color: isDark ? '#D1D5DB' : '#374151' }}>
            Enable WhatsApp automation
          </span>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1" style={{ color: isDark ? '#D1D5DB' : '#374151' }}>
              Message when order is <strong>Confirmed</strong>
            </label>
            <textarea
              value={settings?.whatsappTemplateConfirmed ?? ''}
              onChange={(e) => setSettings((s) => s ? { ...s, whatsappTemplateConfirmed: e.target.value } : null)}
              placeholder={DEFAULT_CONFIRMED}
              rows={5}
              className="w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-0"
              style={{
                backgroundColor: inputBg,
                borderColor: panelBorder,
                color: textPrimary,
                ['--tw-ring-color' as string]: storeColor,
              }}
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1" style={{ color: isDark ? '#D1D5DB' : '#374151' }}>
              Message when order is <strong>Delivered</strong>
            </label>
            <textarea
              value={settings?.whatsappTemplateDelivered ?? ''}
              onChange={(e) => setSettings((s) => s ? { ...s, whatsappTemplateDelivered: e.target.value } : null)}
              placeholder={DEFAULT_DELIVERED}
              rows={5}
              className="w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-0"
              style={{
                backgroundColor: inputBg,
                borderColor: panelBorder,
                color: textPrimary,
                ['--tw-ring-color' as string]: storeColor,
              }}
            />
          </div>
        </div>

        <div
          className="mt-4 p-3 rounded-lg border"
          style={{ backgroundColor: insetBg, borderColor: panelBorder }}
        >
          <p className="text-xs font-medium flex items-center gap-1 mb-2" style={{ color: textSecondary }}>
            <Info className="h-3.5 w-3.5" />
            Placeholders (replace with order data)
          </p>
          <p className="text-xs" style={{ color: textSecondary }}>
            {PLACEHOLDERS.join(', ')}
          </p>
        </div>

        <div className="mt-6">
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
            style={{ backgroundColor: storeColor }}
          >
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Save settings
          </button>
        </div>
      </div>

      {/* Send promotion to all customers */}
      <div
        className="rounded-xl border p-6 shadow-sm"
        style={{ backgroundColor: panelBg, borderColor: panelBorder }}
      >
        <h2 className="text-lg font-semibold mb-2" style={{ color: textPrimary }}>Send promotion to all customers</h2>
        <p className="text-sm mb-4" style={{ color: textSecondary }}>
          Write a message and send it to every customer who has placed an order in this store (one message per unique phone number). Wait until you see the green message “Your WhatsApp is linked” above before sending.
        </p>
        <PromoBroadcast storeId={storeId} storeColor={storeColor} disabled={!settings?.sendingConfigured} linkReady={linkStatus?.provider === 'bridge' ? linkStatus?.ready : true} />
      </div>
    </div>
  )
}

function PromoBroadcast({ storeId, storeColor, disabled, linkReady }: { storeId: string; storeColor: string; disabled: boolean; linkReady?: boolean }) {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const textPrimary = isDark ? '#F9FAFB' : '#111827'
  const textSecondary = isDark ? '#9CA3AF' : '#6B7280'
  const panelBorder = isDark ? '#3d4048' : '#E5E7EB'
  const inputBg = isDark ? '#222328' : '#FFFFFF'
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [result, setResult] = useState<{
    total: number
    sent: number
    failed: number
    failedReasons?: string[]
  } | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const showNotReadyHint = result?.failedReasons?.some((r) => r.includes('not ready') || r.includes('Link your number')) ?? false

  const handleSend = async () => {
    if (!message.trim()) return
    setConfirmOpen(false)
    setSending(true)
    setErr(null)
    setResult(null)
    try {
      const res = await storeService.sendWhatsAppBroadcast(storeId, message.trim())
      setResult(res)
    } catch (e: any) {
      setErr(e.response?.data?.error || e.message || 'Failed to send')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="space-y-3">
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="e.g. Bonjour ! Nouvelle promo -20% ce week-end. Code: PROMO20"
        rows={4}
        className="w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-0"
        style={{
          backgroundColor: inputBg,
          borderColor: panelBorder,
          color: textPrimary,
          ['--tw-ring-color' as string]: storeColor,
        }}
        disabled={disabled}
      />
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => message.trim() && setConfirmOpen(true)}
          disabled={disabled || (linkReady === false) || !message.trim() || sending}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
          style={{ backgroundColor: storeColor }}
        >
          {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
          Send to all customers
        </button>
        {result && (
          <div className="flex flex-col gap-1">
            <span className="text-sm" style={{ color: textSecondary }}>
              Sent: {result.sent} / {result.total} {result.total > 0 && `(${result.total} customer${result.total === 1 ? '' : 's'} with phone number${result.total === 1 ? '' : 's'})`}
              {result.failed > 0 && (
                <span className="text-amber-600 dark:text-amber-400 ml-1">— {result.failed} failed</span>
              )}
            </span>
            {result.failedReasons && result.failedReasons.length > 0 && (
              <ul className="text-xs text-amber-700 dark:text-amber-300 mt-1 list-disc list-inside space-y-0.5">
                {result.failedReasons.slice(0, 5).map((reason, i) => (
                  <li key={i}>{reason}</li>
                ))}
                {result.failedReasons.length > 5 && (
                  <li>… and {result.failedReasons.length - 5} more</li>
                )}
              </ul>
            )}
            {showNotReadyHint && (
              <div className="mt-2 p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 text-sm text-blue-800 dark:text-blue-200">
                <p className="font-medium">Your phone is linked, but the app is not ready yet.</p>
                <p className="mt-1">Wait until you see the green message <strong>“Your WhatsApp is linked”</strong> at the top of this page (the QR will disappear), then try sending again. If you use Docker, run only the bridge inside Docker—do not run <code className="bg-blue-100 dark:bg-blue-900/40 px-1 rounded">npm start</code> in whatsapp-bridge separately.</p>
              </div>
            )}
          </div>
        )}
      </div>
      {err && (
        <p className="text-sm text-red-600 dark:text-red-400">{err}</p>
      )}
      {confirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setConfirmOpen(false)}>
          <div
            className="rounded-xl shadow-xl max-w-md w-full p-6 border"
            style={{ backgroundColor: isDark ? '#2A2D35' : '#FFFFFF', borderColor: panelBorder }}
            onClick={(e) => e.stopPropagation()}
          >
            <p className="font-medium mb-2" style={{ color: textPrimary }}>Send to all customers?</p>
            <p className="text-sm mb-4" style={{ color: textSecondary }}>
              This will send your message to every unique customer phone number from this store&apos;s orders. This may take a while (about 2 seconds per recipient).
            </p>
            <div className="flex gap-3 justify-end">
              <button
                type="button"
                onClick={() => setConfirmOpen(false)}
                className="px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-100 dark:hover:bg-[#3d4048]"
                style={{ color: isDark ? '#D1D5DB' : '#374151' }}
              >
                Cancel
              </button>
              <button type="button" onClick={handleSend} className="px-4 py-2 rounded-lg text-sm font-medium text-white" style={{ backgroundColor: storeColor }}>
                Send
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
