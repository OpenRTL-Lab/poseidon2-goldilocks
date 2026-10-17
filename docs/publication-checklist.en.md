<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/publication-checklist.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Release contents and checks

[简体中文](publication-checklist.md)

This version supplies a standalone Goldilocks Poseidon2 permutation, reproducible verification, bilingual documentation and MIT licensing.

## Design and verification

- The bundled product uses 16 contexts; the generator also supports 32 contexts with functional verification. Synthesis figures apply only to the documented 16-context configuration.
- Public Chisel, product SV, C/Python references and verification scripts are included. Regenerated and bundled SV pass the six documented modes with matching measurements.
- The native boundary is a state-to-state permutation, not a sponge, byte hash, Merkle engine or complete proof system.
- Reproduction commands, pass criteria and tested configurations are specified in the reproduction document. Synthesis summaries specify tools, corner, constraints, WNS/TNS, area and logic-depth definitions.

## Documentation and downloads

The default README.md is English, with the complete Chinese translation in README.zh-CN.md. Every other authored Markdown also has a separate translation, connected by reciprocal language links. Release provides the hardware source archive and bilingual design, evidence and download information. Product file lists exclude verification environments.

## Public-file inspection

```bash
python3 tools/public_audit.py
```

The audit checks the file allowlist, product lists, translation pairs, relative links, credential patterns and disclosure boundaries. Payloads exclude machine paths, execution logs, private records, tool caches and licensed synthesis materials. Automatic inspection does not replace license and disclosure review.

## Licensing and scope

The root LICENSE and applicable third-party MIT notices define redistribution terms. Legitimate mathematical constants and required attribution remain part of the deliverable. Commercial tools, process libraries, original synthesis scripts/reports, mapped netlists and synthesis snapshots are not distributed. Simulation and synthesis do not establish board or silicon qualification.
