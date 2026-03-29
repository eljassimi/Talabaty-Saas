import { useState, useEffect } from 'react';
import { Check, Unplug } from 'lucide-react';
import { useStoreColor } from '../../hooks/useStoreColor';
import { useAuth } from '../../contexts/AuthContext';
import YouCanIntegration from '../YouCanIntegration';
import { youcanService, YouCanStore } from '../../services/youcanService';
import youcanLogo from '../../images/youcan-logo.png';
export default function LeadsSettings() {
    const { storeColor } = useStoreColor();
    const { user } = useAuth();
    const [youcanConnected, setYoucanConnected] = useState(false);
    const [connectedYouCanStore, setConnectedYouCanStore] = useState<YouCanStore | null>(null);
    const [disconnectingYouCan, setDisconnectingYouCan] = useState(false);
    useEffect(() => {
        if (user?.selectedStoreId) {
            checkYouCanConnection();
        }
    }, [user?.selectedStoreId]);
    useEffect(() => {
        const interval = setInterval(() => {
            if (user?.selectedStoreId) {
                checkYouCanConnection();
            }
        }, 5000);
        return () => clearInterval(interval);
    }, [user?.selectedStoreId]);
    const checkYouCanConnection = async () => {
        if (!user?.selectedStoreId)
            return;
        try {
            const stores = await youcanService.getConnectedStores();
            const forStore = stores.find(s => s.storeId === user.selectedStoreId);
            setYoucanConnected(!!forStore);
            setConnectedYouCanStore(forStore ?? null);
        }
        catch (error) {
            console.error('Error checking YouCan connection:', error);
            setYoucanConnected(false);
            setConnectedYouCanStore(null);
        }
    };
    const handleDisconnectYouCan = async () => {
        if (!connectedYouCanStore || disconnectingYouCan)
            return;
        if (!confirm('Disconnect YouCan from this store? You can reconnect anytime.'))
            return;
        setDisconnectingYouCan(true);
        try {
            await youcanService.disconnectStore(connectedYouCanStore.id);
            await checkYouCanConnection();
        }
        catch (error) {
            console.error('Error disconnecting YouCan:', error);
            alert('Failed to disconnect. Please try again.');
        }
        finally {
            setDisconnectingYouCan(false);
        }
    };
    const leadCollectors = [
        {
            id: 'youcan',
            name: 'YouCan',
            description: 'E-commerce platform',
            icon: 'y',
            iconBg: '#ec4899',
            logoUrl: youcanLogo,
            connected: youcanConnected,
            component: 'youcan',
        },
        {
            id: 'shopify',
            name: 'Shopify',
            description: 'E-commerce platform',
            icon: 'S',
            iconBg: '#1f2937',
            logoUrl: 'https://cdn.shopify.com/shopifycloud/brochure/assets/brand-assets/shopify-logo-shopping-bag-full-color-66166b2e55d67988b56b4bd28b63c271e2b9713358cb723070a92bde17ad7d63.svg',
            connected: false,
            component: 'shopify',
        },
        {
            id: 'lightfunnels',
            name: 'Lightfunnels',
            description: 'E-commerce platform.',
            icon: 'LF',
            iconBg: '#1f2937',
            logoUrl: 'https://image.crisp.chat/avatar/website/5a6d16d6-72d4-4240-bd2f-c7cf71c87083/60/?1764848253754',
            connected: false,
            component: 'lightfunnels',
        },
        {
            id: 'wordpress',
            name: 'WordPress',
            description: 'Content management system with WooCommerce',
            icon: 'WP',
            iconBg: '#1f2937',
            logoUrl: 'https://i0.wp.com/wordpress.org/files/2023/02/wmark.png?w=500&ssl=1',
            connected: false,
            component: 'wordpress',
        },
        {
            id: 'file-upload',
            name: 'File Upload',
            description: 'Upload your own CSV file.',
            icon: '📄',
            iconBg: '#1f2937',
            connected: false,
            component: 'file-upload',
        },
    ];
    const handleCollectorClick = (collector: typeof leadCollectors[0]) => {
        if (collector.component === 'youcan') {
            return;
        }
        if (collector.component === 'shopify' || collector.component === 'lightfunnels' || collector.component === 'wordpress') {
            alert(`${collector.name} integration coming soon!`);
        }
    };
    return (<div className="space-y-6">
      <div className="bg-white rounded-xl shadow-sm border p-6" style={{
            borderColor: storeColor + '15',
            boxShadow: `0 1px 3px 0 ${storeColor}08`
        }}>
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-2" style={{ color: storeColor }}>
            Connect your Lead Collector
          </h2>
          <p className="text-sm text-gray-600">Choose your lead collector to link it.</p>
        </div>

        
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {leadCollectors.map((collector) => (<div key={collector.id} onClick={() => handleCollectorClick(collector)} className={`border rounded-xl p-5 hover:shadow-lg transition-all duration-300 cursor-pointer relative bg-white group ${collector.connected
                ? 'border-green-300 shadow-md'
                : 'border-gray-200 hover:border-opacity-60'}`} style={collector.connected ? {
                borderColor: storeColor + '40',
                boxShadow: `0 4px 6px -1px ${storeColor}15, 0 2px 4px -1px ${storeColor}10`
            } : {}} onMouseEnter={(e) => {
                if (!collector.connected) {
                    e.currentTarget.style.borderColor = storeColor + '60';
                    e.currentTarget.style.transform = 'translateY(-2px)';
                }
            }} onMouseLeave={(e) => {
                if (!collector.connected) {
                    e.currentTarget.style.borderColor = '';
                    e.currentTarget.style.transform = '';
                }
            }}>
              {collector.connected && (<div className="absolute -top-2 -left-2 z-10">
                  <div className="rounded-full p-1 shadow-lg" style={{ backgroundColor: storeColor }}>
                    <Check className="h-3.5 w-3.5 text-white"/>
                  </div>
                </div>)}
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4 flex-1">
                  {collector.logoUrl ? (<div className="h-14 w-14 rounded-xl flex items-center justify-center bg-gradient-to-br from-white to-gray-50 border-2 overflow-hidden p-1.5 transition-all duration-300 group-hover:scale-110" style={collector.connected ? {
                    borderColor: storeColor + '30',
                    boxShadow: `0 2px 8px ${storeColor}20`
                } : {
                    borderColor: '#e5e7eb'
                }}>
                      <img src={collector.logoUrl} alt={collector.name} className="h-full w-full object-contain" onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.style.display = 'none';
                    const parent = target.parentElement;
                    if (parent) {
                        parent.innerHTML = `<div class="h-full w-full flex items-center justify-center text-white font-bold text-lg rounded-lg" style="background: linear-gradient(135deg, ${collector.iconBg || '#1f2937'} 0%, ${collector.iconBg || '#1f2937'}dd 100%);">${collector.icon}</div>`;
                    }
                }}/>
                    </div>) : (<div className="h-14 w-14 rounded-xl flex items-center justify-center text-white font-bold text-lg transition-all duration-300 group-hover:scale-110 shadow-md" style={{
                    background: collector.connected
                        ? `linear-gradient(135deg, ${storeColor} 0%, ${storeColor}dd 100%)`
                        : `linear-gradient(135deg, ${collector.iconBg || '#1f2937'} 0%, ${collector.iconBg || '#1f2937'}dd 100%)`
                }}>
                      {collector.icon}
                    </div>)}
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-semibold truncate mb-0.5 transition-colors" style={collector.connected ? { color: storeColor } : { color: '#111827' }}>
                      {collector.name}
                    </h3>
                    <p className="text-xs text-gray-500 truncate">{collector.description}</p>
                  </div>
                </div>
                {!collector.connected && (<div className="opacity-0 group-hover:opacity-100 transition-opacity duration-300" style={{ color: storeColor }}>
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/>
                    </svg>
                  </div>)}
              </div>
            </div>))}
        </div>
      </div>

      
      {user?.selectedStoreId && !youcanConnected && (<div className="bg-white rounded-xl shadow-sm border p-6" style={{
                borderColor: storeColor + '20',
                boxShadow: `0 1px 3px 0 ${storeColor}10`
            }}>
          <h3 className="text-lg font-semibold mb-4" style={{ color: storeColor }}>
            YouCan Integration
          </h3>
          <YouCanIntegration storeId={user.selectedStoreId} onConnectionChange={() => checkYouCanConnection()}/>
        </div>)}

      
      {youcanConnected && (<div className="bg-white rounded-xl shadow-sm border p-6" style={{
                borderColor: storeColor + '20',
                boxShadow: `0 1px 3px 0 ${storeColor}10`
            }}>
          <h3 className="text-lg font-semibold mb-4" style={{ color: storeColor }}>
            Connected Integrations
          </h3>
          <div className="space-y-3">
            {youcanConnected && (<div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4 border rounded-xl transition-all duration-300 hover:shadow-md" style={{
                    borderColor: storeColor + '30',
                    backgroundColor: storeColor + '08'
                }}>
                <div className="flex items-center space-x-3">
                  <div className="h-12 w-12 rounded-xl flex items-center justify-center bg-white border-2 overflow-hidden p-1.5 shadow-sm shrink-0" style={{ borderColor: storeColor + '30' }}>
                    <img src={youcanLogo} alt="YouCan" className="h-full w-full object-contain" onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.style.display = 'none';
                    const parent = target.parentElement;
                    if (parent) {
                        parent.innerHTML = `<div class="h-full w-full flex items-center justify-center text-white font-bold text-sm rounded-lg" style="background: linear-gradient(135deg, ${storeColor} 0%, ${storeColor}dd 100%);">Y</div>`;
                    }
                }}/>
                  </div>
                  <div>
                    <p className="text-sm font-semibold" style={{ color: storeColor }}>
                      YouCan
                    </p>
                    <p className="text-xs text-gray-500">E-commerce platform</p>
                  </div>
                </div>
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-3 border-t border-gray-200 pt-3 sm:pt-0 sm:border-t-0">
                  <div className="flex items-center" style={{ color: storeColor }}>
                    <div className="rounded-full p-1 mr-2" style={{ backgroundColor: storeColor + '20' }}>
                      <Check className="h-4 w-4" style={{ color: storeColor }}/>
                    </div>
                    <span className="text-sm font-medium">Connected</span>
                  </div>
                  <button type="button" onClick={(e) => { e.stopPropagation(); handleDisconnectYouCan(); }} disabled={disconnectingYouCan} className="flex items-center justify-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-red-500 hover:bg-red-600 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm">
                    <Unplug className="h-4 w-4"/>
                    {disconnectingYouCan ? 'Disconnecting…' : 'Disconnect'}
                  </button>
                </div>
              </div>)}
          </div>
        </div>)}
    </div>);
}
