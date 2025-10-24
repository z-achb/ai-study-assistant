import { useState } from 'react'
import api from '../api/client'

export default function UploadForm() {
  const [file, setFile] = useState(null)
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)

  const handleUpload = async () => {
    if (!file) {
      setStatus('Select a PDF first.')
      return
    }
    const formData = new FormData()
    formData.append('file', file)
    setBusy(true)
    setStatus('')

    try {
      const res = await api.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setStatus(`Uploaded: ${res.data?.filename ?? file.name}`)
    } catch (err) {
      const status = err.response?.status
      const text = typeof err.response?.data === 'string'
        ? err.response.data
        : JSON.stringify(err.response?.data)
      setStatus(`Upload failed (HTTP ${status ?? 'n/a'}): ${text || err.message}`)
      console.error('Upload error:', err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <div className="label">Document (PDF)</div>
      <div className="input">
        <input
          type="file"
          accept="application/pdf"
          onChange={(e) => setFile(e.target.files[0])}
        />
        <button className="btn" onClick={handleUpload} disabled={busy}>
          {busy ? 'Uploading…' : 'Upload'}
        </button>
      </div>
      {status && (
        <div className={`status ${status.startsWith('Uploaded') ? 'ok' : status.startsWith('Upload failed') ? 'err' : ''}`}>
          {status}
        </div>
      )}
    </div>
  )
}
