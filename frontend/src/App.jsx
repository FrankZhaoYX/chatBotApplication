import React from 'react';
import ChatWindow from './components/ChatWindow';
import MessageInput from './components/MessageInput';
import './App.css';

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>ChatBot</h1>
      </header>
      <main className="app-main">
        <ChatWindow />
        <MessageInput />
      </main>
    </div>
  );
}
