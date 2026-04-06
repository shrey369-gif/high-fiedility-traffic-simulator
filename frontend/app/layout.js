"use client";
import "./globals.css";
import Navbar from "@/components/Navbar";

export default function RootLayout({ children }) {
  return (
    <html lang="en" className="h-full antialiased">
      <head>
        <title>Indian Traffic Simulator — High-Fidelity Road Network Modeling</title>
        <meta name="description" content="Accelerating High-Fidelity Road Network Modeling for Indian Traffic Simulations. A real-time simulation engine demonstrating realistic Indian traffic behavior." />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
      </head>
      <body className="min-h-full flex flex-col noise">
        <Navbar />
        <main className="flex-1">{children}</main>
      </body>
    </html>
  );
}
