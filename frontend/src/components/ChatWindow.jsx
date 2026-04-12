import React, { useEffect, useRef } from 'react';
import { useSelector } from 'react-redux';
import './ChatWindow.css';

export default function ChatWindow() {
  const messages = useSelector((state) => state.chat.messages);
  const streaming = useSelector((state) => state.chat.streaming);
  const error = useSelector((state) => state.chat.error);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="chat-window">
      {messages.map((msg) => (
        <div key={msg.id} className={`message message--${msg.role}`}>
          <span className="message__role">{msg.role === 'user' ? 'You' : 'AI'}</span>
          <p className="message__content">{msg.content}</p>
        </div>
      ))}
      {streaming && <div className="typing-indicator">AI is typing...</div>}
      {error && <div className="error-banner">Error: {error}</div>}
      <div ref={bottomRef} />
    </div>
  );
}
