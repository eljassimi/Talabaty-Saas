import { useState, useEffect } from 'react'
import { X, AlertCircle, CheckCircle, RefreshCw } from 'lucide-react'
import { googleSheetsSyncService, GoogleSheetsSyncConfig, CreateSyncConfigRequest, UpdateSyncConfigRequest, SyncResult } from '../services/googleSheetsSyncService'
import { useStoreColor } from '../hooks/useStoreColor'

interface GoogleSheetsSyncModalProps {
  storeId: string
  config: GoogleSheetsSyncConfig | null
  onClose: () => void
  onSuccess: () => void
}

export default function GoogleSheetsSyncModal({ storeId, config, onClose, onSuccess }: GoogleSheetsSyncModalProps) {
  const { storeColor, hexToRgb } = useStoreColor()
  const [loading, setLoading] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [syncResult, setSyncResult] = useState<SyncResult | null>(null)
  
  const [formData, setFormData] = useState<CreateSyncConfigRequest | UpdateSyncConfigRequest>({
    spreadsheetId: config?.spreadsheetId || '',
    sheetName: config?.sheetName || 'Sheet1',
    credentialsJson: '',
    syncEnabled: config?.syncEnabled ?? true,
    syncIntervalSeconds: config?.syncIntervalSeconds || 30,
    columnMapping: config?.columnMapping || '',
  })

  useEffect(() => {
    if (config) {
      setFormData({
        spreadsheetId: config.spreadsheetId,
        sheetName: config.sheetName,
        syncEnabled: config.syncEnabled,
        syncIntervalSeconds: config.syncIntervalSeconds,
        columnMapping: config.columnMapping || '',
      })
    }
  }, [config])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSyncResult(null)

    try {
      if (config) {
        // Update existing config
        await googleSheetsSyncService.updateConfig(config.id, formData as UpdateSyncConfigRequest)
      } else {
        // Create new config
        await googleSheetsSyncService.createConfig({
          ...formData,
          storeId,
        } as CreateSyncConfigRequest)
      }
      onSuccess()
      onClose()
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to save configuration')
    } finally {
      setLoading(false)
    }
  }

  const handleSync = async () => {
    if (!config) return
    
    setSyncing(true)
    setError(null)
    setSyncResult(null)

    try {
      const result = await googleSheetsSyncService.triggerSync(config.id)
      setSyncResult(result)
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to sync')
    } finally {
      setSyncing(false)
    }
  }

  const extractSpreadsheetId = (url: string) => {
    // Extract spreadsheet ID from Google Sheets URL
    // Format: https://docs.google.com/spreadsheets/d/{SPREADSHEET_ID}/edit
    const match = url.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/)
    return match ? match[1] : url
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-xl font-bold text-gray-900">
            {config ? 'Modifier la synchronisation Google Sheets' : 'Lier un Google Sheet'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-start">
              <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {syncResult && (
            <div className={`border px-4 py-3 rounded-lg text-sm ${
              syncResult.success 
                ? 'bg-green-50 border-green-200 text-green-700' 
                : 'bg-red-50 border-red-200 text-red-700'
            }`}>
              <div className="flex items-start">
                {syncResult.success ? (
                  <CheckCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
                ) : (
                  <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
                )}
                <div>
                  <p className="font-medium">{syncResult.message}</p>
                  {syncResult.success && (
                    <p className="mt-1 text-xs">
                      {syncResult.created} créé(s), {syncResult.updated} mis à jour(s), {syncResult.errors} erreur(s)
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              URL ou ID du Google Sheet *
            </label>
            <input
              type="text"
              value={formData.spreadsheetId || ''}
              onChange={(e) => {
                const value = e.target.value
                setFormData({ ...formData, spreadsheetId: extractSpreadsheetId(value) })
              }}
              placeholder="https://docs.google.com/spreadsheets/d/... ou l'ID du sheet"
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
              style={{
                '--tw-ring-color': storeColor,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = ''
                e.currentTarget.style.boxShadow = ''
              }}
              required
            />
            <p className="mt-1 text-xs text-gray-500">
              Collez l'URL complète du Google Sheet ou juste l'ID du document
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nom de la feuille
            </label>
            <input
              type="text"
              value={formData.sheetName || 'Sheet1'}
              onChange={(e) => setFormData({ ...formData, sheetName: e.target.value })}
              placeholder="Sheet1"
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
              style={{
                '--tw-ring-color': storeColor,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = ''
                e.currentTarget.style.boxShadow = ''
              }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Credentials JSON (OAuth2)
            </label>
            <textarea
              value={formData.credentialsJson || ''}
              onChange={(e) => setFormData({ ...formData, credentialsJson: e.target.value })}
              placeholder='Collez le contenu du fichier credentials.json de Google Cloud Console'
              rows={6}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all resize-none font-mono text-xs"
              style={{
                '--tw-ring-color': storeColor,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = ''
                e.currentTarget.style.boxShadow = ''
              }}
            />
            <p className="mt-1 text-xs text-gray-500">
              Obtenez ce fichier depuis Google Cloud Console après avoir créé un projet et activé l'API Google Sheets
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Intervalle de synchronisation (secondes)
              </label>
              <input
                type="number"
                value={formData.syncIntervalSeconds || 30}
                onChange={(e) => setFormData({ ...formData, syncIntervalSeconds: parseInt(e.target.value) || 30 })}
                min="10"
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all"
                style={{
                  '--tw-ring-color': storeColor,
                } as React.CSSProperties & { '--tw-ring-color': string }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = storeColor
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = ''
                  e.currentTarget.style.boxShadow = ''
                }}
              />
            </div>

            <div className="flex items-end">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.syncEnabled ?? true}
                  onChange={(e) => setFormData({ ...formData, syncEnabled: e.target.checked })}
                  className="w-4 h-4 rounded border-gray-300"
                  style={{ accentColor: storeColor }}
                />
                <span className="text-sm font-medium text-gray-700">Synchronisation activée</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Mapping des colonnes (JSON optionnel)
            </label>
            <textarea
              value={formData.columnMapping || ''}
              onChange={(e) => setFormData({ ...formData, columnMapping: e.target.value })}
              placeholder='{"customerName": 0, "customerPhone": 1, "destinationAddress": 2, ...}'
              rows={3}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:border-transparent transition-all resize-none font-mono text-xs"
              style={{
                '--tw-ring-color': storeColor,
              } as React.CSSProperties & { '--tw-ring-color': string }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = storeColor
                e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = ''
                e.currentTarget.style.boxShadow = ''
              }}
            />
            <p className="mt-1 text-xs text-gray-500">
              Laissez vide pour auto-détection. Format: {"{"}"customerName": 0, "customerPhone": 1{"}"}
            </p>
          </div>

          {config && (
            <div className="bg-gray-50 rounded-lg p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Dernière synchronisation</span>
                <span className={`text-xs font-semibold px-2 py-1 rounded ${
                  config.lastSyncStatus === 'SUCCESS' 
                    ? 'bg-green-100 text-green-800' 
                    : config.lastSyncStatus === 'ERROR'
                    ? 'bg-red-100 text-red-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {config.lastSyncStatus || 'Jamais'}
                </span>
              </div>
              {config.lastSyncAt && (
                <p className="text-xs text-gray-500">
                  {new Date(config.lastSyncAt).toLocaleString()}
                </p>
              )}
              {config.lastSyncError && (
                <p className="text-xs text-red-600">{config.lastSyncError}</p>
              )}
              <button
                type="button"
                onClick={handleSync}
                disabled={syncing}
                className="w-full inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ backgroundColor: storeColor }}
                onMouseEnter={(e) => {
                  if (!syncing) {
                    const rgb = hexToRgb(storeColor)
                    e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = storeColor
                }}
              >
                {syncing ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Synchronisation...
                  </>
                ) : (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2" />
                    Synchroniser maintenant
                  </>
                )}
              </button>
            </div>
          )}

          <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              style={{ backgroundColor: storeColor }}
              onMouseEnter={(e) => {
                if (!loading) {
                  const rgb = hexToRgb(storeColor)
                  e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = storeColor
              }}
            >
              {loading ? 'Enregistrement...' : config ? 'Mettre à jour' : 'Lier le Google Sheet'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

