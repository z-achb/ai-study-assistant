import UploadForm from './components/UploadForm'
import QueryBox from './components/QueryBox'
import './styles.css'

export default function App() {
  return (
    <div className="app">
      <div className="shell">
        <header>
          <h1 className="glow-title">AI Study Assistant</h1>
          <p className="sub">Upload a PDF, then ask questions about it.</p>
        </header>

        <div className="content">
          <div className="card"><UploadForm /></div>
          <div className="card"><QueryBox /></div>
        </div>
      </div>
    </div>
  )
}
