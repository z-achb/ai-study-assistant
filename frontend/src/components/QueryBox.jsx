import { useState } from 'react'
import api from '../api/client'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export default function QueryBox() {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleQuery = async () => {
    if (!question.trim()) {
      setError('Please enter a question first.')
      return
    }

    setLoading(true)
    setAnswer('')
    setError('')

    try {
      const res = await api.post('/query', {
        question,
        topK: 3
      })

      setAnswer(res.data.answer || '(No answer returned)')
      setSources(res.data.sources || [])
      setQuestion('') // Clear the text area after successful query
    } catch (err) {
      console.error(err)
      const msg =
        err.response?.data?.message ||
        err.response?.data ||
        err.message ||
        'An unexpected error occurred.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  // Optional: allow pressing Enter+Ctrl to send
  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      handleQuery()
    }
  }

  return (
    <div>
      <div className="label">Ask a Question:</div>
      <textarea
        rows={3}
        placeholder="Summarize this information..."
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        onKeyDown={handleKeyDown}
        style={{
          width: '100%',
          resize: 'vertical',
          background: '#0b132a',
          color: 'white',
          borderRadius: '10px',
          border: '1px solid #223055',
          padding: '8px',
          fontSize: '14px',
        }}
      />
      <button
        className="btn"
        onClick={handleQuery}
        disabled={loading}
        style={{
          marginTop: '10px',
          opacity: loading ? 0.7 : 1,
          cursor: loading ? 'not-allowed' : 'pointer',
        }}
      >
        {loading ? 'Asking...' : 'Ask'}
      </button>

      {error && <div className="status err">{error}</div>}

      {answer && (
        <div style={{ marginTop: '1rem' }}>
          <h4>Answer</h4>
          <div className="answer">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {answer}
            </ReactMarkdown>
          </div>

          {sources.length > 0 && (
            <>
              <h5 style={{ marginTop: '1rem' }}>Sources</h5>
              <ul>
                {sources.map((s) => (
                  <li key={s.chunkId}>
                    <strong>{s.documentName}</strong> — chunk {s.chunkIndex}{' '}
                    (similarity {s.similarity?.toFixed?.(2) ?? 'n/a'})
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  )
}
