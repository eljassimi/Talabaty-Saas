import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { storeService, Store } from '../services/storeService';
import { Plus, Store as StoreIcon, Edit, Trash2, Search, ArrowRight, Calendar } from 'lucide-react';
import CreateStoreModal from '../components/CreateStoreModal';
import { useStoreColor } from '../hooks/useStoreColor';
function hexToRgb(hex: string) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : { r: 2, g: 132, b: 199 };
}
export default function Stores() {
    const { storeColor } = useStoreColor();
    const [stores, setStores] = useState<Store[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    useEffect(() => {
        loadStores();
    }, []);
    const loadStores = async () => {
        try {
            const data = await storeService.getStores();
            setStores(Array.isArray(data) ? data : []);
        }
        catch (error) {
            console.error('Error loading stores:', error);
        }
        finally {
            setLoading(false);
        }
    };
    const handleDelete = async (id: string) => {
        if (!confirm('Are you sure you want to delete this store?'))
            return;
        try {
            await storeService.deleteStore(id);
            loadStores();
        }
        catch (error) {
            console.error('Error deleting store:', error);
            alert('Failed to delete store');
        }
    };
    const filteredStores = stores.filter((store) => {
        const query = searchQuery.toLowerCase();
        return (store.name?.toLowerCase().includes(query) ||
            store.code?.toLowerCase().includes(query));
    });
    if (loading) {
        return (<div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2" style={{ borderColor: storeColor }}></div>
      </div>);
    }
    return (<div className="space-y-6">
      
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Stores</h1>
          <p className="mt-2 text-gray-600">Manage your stores and their settings</p>
        </div>
        <button onClick={() => setShowCreateModal(true)} className="inline-flex items-center px-4 py-2.5 border border-transparent shadow-sm text-sm font-medium rounded-lg text-white transition-all hover:shadow-lg" style={{ backgroundColor: storeColor }} onMouseEnter={(e) => {
            const rgb = hexToRgb(storeColor);
            e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`;
        }} onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = storeColor;
        }}>
          <Plus className="h-5 w-5 mr-2"/>
          New Store
        </button>
      </div>

      
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400"/>
          <input type="text" placeholder="Search by name or code..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 transition-all" style={{
            '--tw-ring-color': storeColor,
        } as React.CSSProperties & {
            '--tw-ring-color': string;
        }} onFocus={(e) => {
            e.currentTarget.style.borderColor = storeColor;
            e.currentTarget.style.boxShadow = `0 0 0 2px ${storeColor}40`;
        }} onBlur={(e) => {
            e.currentTarget.style.borderColor = '';
            e.currentTarget.style.boxShadow = '';
        }}/>
        </div>
      </div>

      
      {filteredStores.length === 0 ? (<div className="bg-white rounded-xl shadow-sm border border-gray-100 px-6 py-12 text-center">
          <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <StoreIcon className="h-8 w-8 text-gray-400"/>
          </div>
          <h3 className="text-sm font-medium text-gray-900">No stores found</h3>
          <p className="mt-1 text-sm text-gray-500">
            {searchQuery ? 'Try adjusting your search criteria.' : 'Get started by creating a new store.'}
          </p>
          {!searchQuery && (<div className="mt-6">
              <button onClick={() => setShowCreateModal(true)} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-lg text-white transition-all" style={{ backgroundColor: storeColor }} onMouseEnter={(e) => {
                    const rgb = hexToRgb(storeColor);
                    e.currentTarget.style.backgroundColor = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`;
                }} onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = storeColor;
                }}>
                <Plus className="h-5 w-5 mr-2"/>
                New Store
              </button>
            </div>)}
        </div>) : (<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredStores.map((store) => (<div key={store.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-lg transition-all card-hover" style={{
                    borderTopColor: store.color || storeColor,
                    borderTopWidth: '4px'
                }}>
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-4">
                  {store.logoUrl ? (<img src={store.logoUrl} alt={store.name} className="w-16 h-16 object-cover rounded-xl shadow-lg border-2 border-gray-200"/>) : (<div className="p-4 rounded-xl shadow-lg" style={{ backgroundColor: store.color || storeColor }}>
                      <StoreIcon className="h-6 w-6 text-white"/>
                    </div>)}
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-semibold text-gray-900 truncate">{store.name}</h3>
                    <p className="text-sm text-gray-500 mt-1">Code: {store.code}</p>
                  </div>
                </div>
              </div>

              <div className="space-y-3 mb-4">
                {store.timezone && (<div className="flex items-center text-sm text-gray-600">
                    <Calendar className="h-4 w-4 mr-2 text-gray-400"/>
                    <span>{store.timezone}</span>
                  </div>)}
                <div className="flex items-center text-sm text-gray-600">
                  <Calendar className="h-4 w-4 mr-2 text-gray-400"/>
                  <span>Created: {new Date(store.createdAt).toLocaleDateString()}</span>
                </div>
              </div>

              <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                <Link to={`/stores/${store.id}`} className="inline-flex items-center text-sm font-medium transition-colors" style={{ color: storeColor }} onMouseEnter={(e) => {
                    const rgb = hexToRgb(storeColor);
                    e.currentTarget.style.color = `rgb(${Math.max(0, rgb.r - 20)}, ${Math.max(0, rgb.g - 20)}, ${Math.max(0, rgb.b - 20)})`;
                }} onMouseLeave={(e) => {
                    e.currentTarget.style.color = storeColor;
                }}>
                  View Details
                  <ArrowRight className="h-4 w-4 ml-1"/>
                </Link>
                <div className="flex items-center space-x-2">
                  <Link to={`/stores/${store.id}`} className="p-2 text-gray-400 hover:bg-gray-50 rounded-lg transition-all" style={{
                    color: storeColor,
                }} title="Edit">
                    <Edit className="h-5 w-5"/>
                  </Link>
                  <button onClick={() => handleDelete(store.id)} className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all" title="Delete">
                    <Trash2 className="h-5 w-5"/>
                  </button>
                </div>
              </div>
            </div>))}
        </div>)}

      {showCreateModal && (<CreateStoreModal onClose={() => setShowCreateModal(false)} onSuccess={() => {
                setShowCreateModal(false);
                loadStores();
            }}/>)}
    </div>);
}
