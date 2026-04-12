import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { sendMessage, clearChat } from '../store/chatSlice';
import './MessageInput.css';

export default function MessageInput() {
  const [text, setText] = useState('');
  const dispatch = useDispatch();
  const streaming = useSelector((state) => state.chat.streaming);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!text.trim() || streaming) return;
    dispatch(sendMessage(text.trim()));
    setText('');
  };

  return (
    <form className="message-input" onSubmit={handleSubmit}>
      <textarea
        className="message-input__field"
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) handleSubmit(e);
        }}
        placeholder="Type a message… (Enter to send, Shift+Enter for newline)"
        disabled={streaming}
        rows={3}
      />
      <div className="message-input__actions">
        <button
          type="button"
          className="message-input__btn message-input__btn--clear"
          onClick={() => dispatch(clearChat())}
          disabled={streaming}
        >
          Clear
        </button>
        <button
          type="submit"
          className="message-input__btn message-input__btn--send"
          disabled={streaming || !text.trim()}
        >
          Send
        </button>
      </div>
    </form>
  );
}
