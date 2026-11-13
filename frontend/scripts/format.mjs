#!/usr/bin/env node
/**
 * Formatação com oxfmt.
 *
 * O oxfmt 0.2 não expande globs (no Windows o cmd.exe também não expande),
 * então enumeramos explicitamente os arquivos do projeto. O escopo são os
 * fontes em `src`, os scripts em `scripts` e as configurações do Vite/Vitest —
 * nunca `node_modules` ou `dist`.
 */
import { spawnSync } from "node:child_process"
import { readdirSync } from "node:fs"
import path from "node:path"

const root = process.cwd()
const check = process.argv.includes("--check")

function collect(dir) {
  const files = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (entry.name === "node_modules" || entry.name === "dist") continue
      files.push(...collect(full))
    } else if (entry.name.endsWith(".js") || entry.name.endsWith(".mjs")) {
      files.push(full)
    }
  }
  return files
}

const targets = [
  ...collect(path.join(root, "src")),
  ...collect(path.join(root, "scripts")),
  path.join(root, "vite.config.ts"),
  path.join(root, "vitest.config.ts"),
].map((file) => path.relative(root, file))

if (targets.length === 0) {
  console.error("Nenhum arquivo para formatar.")
  process.exit(1)
}

const oxfmt = path.join(root, "node_modules", "oxfmt", "bin", "oxfmt")
const args = [...(check ? ["--check"] : []), ...targets]
const result = spawnSync(process.execPath, [oxfmt, ...args], {
  stdio: "inherit",
})

if (result.error) {
  console.error(result.error.message)
  process.exit(1)
}

process.exit(result.status ?? 1)
