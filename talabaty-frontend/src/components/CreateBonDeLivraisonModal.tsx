import { useState, useMemo } from 'react'
import { X, Loader2, Copy, ExternalLink } from 'lucide-react'
import type { Order } from '../services/orderService'
import { shippingService, type BonDeLivraisonResult } from '../services/shippingService'

interface CreateBonDeLivraisonModalProps {
  orders: Order[]
  onClose: () => void
  onSuccess?: () => void
  storeColor?: string
}

export default function CreateBonDeLivraisonModal({
  orders,
  onClose,
  onSuccess,
  storeColor = '#0284c7',
}: CreateBonDeLivraisonModalProps) {
  const ordersWithTracking = useMemo(
    () => orders.filter((o) => o.ozonTrackingNumber && o.ozonTrackingNumber.trim()),
    [orders]
  )
  const [selectedIds, setSelectedIds] = useState<Set<string>>(
    () => new Set(ordersWithTracking.map((o) => o.id))
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<BonDeLivraisonResult | null>(null)
  const [copied, setCopied] = useState<string | null>(null)

  const copyLink = (url: string, label: string) => {
    navigator.clipboard.writeText(url).then(() => {
      setCopied(label)
      setTimeout(() => setCopied(null), 2000)
    })
  }

  const openPdfInNewTab = (url: string) => {
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  const toggle = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const selectAll = () => {
    if (selectedIds.size === ordersWithTracking.length) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(ordersWithTracking.map((o) => o.id)))
    }
  }

  const handleCreate = async () => {
    const ids = Array.from(selectedIds)
    if (ids.length === 0) {
      setError('Select at least one order with a tracking number.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const res = await shippingService.createBonDeLivraison(ids)
      setResult(res)
      onSuccess?.()
    } catch (e: any) {
      const data = e.response?.data
      const msg = (data && (data.error || data.message)) || e.message || 'Failed to create Bon de Livraison'
      setError(typeof msg === 'string' ? msg : String(msg))
    } finally {
      setLoading(false)
    }
  }

  if (ordersWithTracking.length === 0) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Bon de Livraison</h2>
            <button type="button" onClick={onClose} className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
              <X className="h-5 w-5" />
            </button>
          </div>
          <p className="text-gray-600">
            No orders with Ozon Express tracking numbers. Send orders to shipping first to get tracking numbers.
          </p>
          <div className="mt-6 flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (result) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">
              {result.existing ? 'Bon de Livraison existant' : 'Bon de Livraison créé'}
            </h2>
            <button type="button" onClick={onClose} className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
              <X className="h-5 w-5" />
            </button>
          </div>
          <p className="text-sm text-gray-600 mb-2">
            Référence: <strong>{result.ref}</strong> ({result.trackingCount} colis)
          </p>
          <p className="text-xs text-gray-600 bg-gray-100 border border-gray-200 rounded-lg px-3 py-2 mb-3">
            Ozon n’autorise l’ouverture du PDF que depuis leur site. Pour télécharger : ouvrez{' '}
            <a href="https://client.ozoneexpress.ma" target="_blank" rel="noopener noreferrer" className="underline font-medium text-blue-600">client.ozoneexpress.ma</a>, connectez-vous si besoin, puis <strong>copiez le lien</strong> ci‑dessous et <strong>collez-le dans la barre d’adresse</strong> de cet onglet.
          </p>
          <div className="space-y-2 mt-4">
            <div className="flex items-center gap-2 rounded-xl border overflow-hidden">
              <button
                type="button"
                onClick={() => copyLink(result.pdfUrl, 'pdf')}
                className="flex-1 flex items-center gap-2 px-4 py-2 text-left text-gray-700 hover:bg-gray-50"
              >
                {copied === 'pdf' ? <span className="text-sm text-green-600 font-medium">Copié !</span> : <Copy className="h-4 w-4 shrink-0" />}
                <span>PDF Standard</span>
              </button>
              <button
                type="button"
                onClick={() => openPdfInNewTab(result.pdfUrl)}
                className="px-3 py-2 border-l bg-gray-50 hover:bg-gray-100 text-gray-500 shrink-0"
                title="Ouvrir (peut afficher 404)"
              >
                <ExternalLink className="h-4 w-4" />
              </button>
            </div>
            <div className="flex items-center gap-2 rounded-xl border overflow-hidden">
              <button
                type="button"
                onClick={() => copyLink(result.pdfTicketsUrl, 'tickets')}
                className="flex-1 flex items-center gap-2 px-4 py-2 text-left text-gray-700 hover:bg-gray-50"
              >
                {copied === 'tickets' ? <span className="text-sm text-green-600 font-medium">Copié !</span> : <Copy className="h-4 w-4 shrink-0" />}
                <span>Étiquettes A4</span>
              </button>
              <button
                type="button"
                onClick={() => openPdfInNewTab(result.pdfTicketsUrl)}
                className="px-3 py-2 border-l bg-gray-50 hover:bg-gray-100 text-gray-500 shrink-0"
                title="Ouvrir (peut afficher 404)"
              >
                <ExternalLink className="h-4 w-4" />
              </button>
            </div>
            <div className="flex items-center gap-2 rounded-xl border overflow-hidden">
              <button
                type="button"
                onClick={() => copyLink(result.pdfTickets4x4Url, 'tickets-4-4')}
                className="flex-1 flex items-center gap-2 px-4 py-2 text-left text-gray-700 hover:bg-gray-50"
              >
                {copied === 'tickets-4-4' ? <span className="text-sm text-green-600 font-medium">Copié !</span> : <Copy className="h-4 w-4 shrink-0" />}
                <span>Étiquettes 10×10 cm</span>
              </button>
              <button
                type="button"
                onClick={() => openPdfInNewTab(result.pdfTickets4x4Url)}
                className="px-3 py-2 border-l bg-gray-50 hover:bg-gray-100 text-gray-500 shrink-0"
                title="Ouvrir (peut afficher 404)"
              >
                <ExternalLink className="h-4 w-4" />
              </button>
            </div>
          </div>
          <div className="mt-6 flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-white"
              style={{ backgroundColor: storeColor }}
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Créer un Bon de Livraison</h2>
          <button type="button" onClick={onClose} className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6 overflow-y-auto flex-1">
          <p className="text-sm text-gray-600 mb-4">
            Sélectionnez les commandes à inclure (avec numéro de suivi Ozon Express). Le BL sera créé, les colis
            ajoutés puis enregistré.
          </p>
          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}
          <div className="flex items-center gap-2 mb-3">
            <button
              type="button"
              onClick={selectAll}
              className="text-sm font-medium px-3 py-1.5 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              {selectedIds.size === ordersWithTracking.length ? 'Tout désélectionner' : 'Tout sélectionner'}
            </button>
            <span className="text-sm text-gray-500">
              {selectedIds.size} / {ordersWithTracking.length} sélectionnés
            </span>
          </div>
          <ul className="border border-gray-200 rounded-xl divide-y divide-gray-200 max-h-64 overflow-y-auto">
            {ordersWithTracking.map((order) => (
              <li key={order.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50">
                <input
                  type="checkbox"
                  checked={selectedIds.has(order.id)}
                  onChange={() => toggle(order.id)}
                  className="rounded border-gray-300"
                />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {order.customerName} — {order.ozonTrackingNumber}
                  </p>
                  <p className="text-xs text-gray-500 truncate">{order.destinationAddress}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
        <div className="flex justify-end gap-2 p-6 border-t border-gray-200">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            Annuler
          </button>
          <button
            type="button"
            onClick={handleCreate}
            disabled={loading || selectedIds.size === 0}
            className="px-4 py-2 rounded-xl text-white disabled:opacity-50 flex items-center gap-2"
            style={{ backgroundColor: storeColor }}
          >
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Création…
              </>
            ) : (
              'Créer le Bon de Livraison'
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
