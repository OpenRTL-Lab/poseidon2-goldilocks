<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/interface.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Native Interface

[简体中文](interface.md)

This document defines the single-clock native handshake interface of `GoldilocksPoseidon2`, its 12-element requests and responses, 32-bit tags, and backpressure, enable, and reset semantics, allowing integrators to connect task inputs and result outputs directly; all handshakes occur on rising clock edges.

Each request is one complete 12-element permutation. Every input word must be an unsigned canonical value 0 <= x < p for p = 2^64 - 2^32 + 1; outputs use the same representation. Noncanonical inputs have no defined behavior. A context is a stored in-flight permutation state sharing the arithmetic pipeline with other contexts, not a separate permutation core. The bundled product has 16 such slots; its request queue is separate from those slots. See [Circuit function and mathematical model](mathematical-model.en.md) for the exact fixed permutation.

| Signal | Direction | Definition |
| --- | --- | --- |
| `clock` | Input | Operating clock |
| `reset` | Input | Synchronous, active-high reset |
| `io_enable` | Input | Allows queued tasks to enter idle contexts |
| `io_request_valid` | Input | Request is valid |
| `io_request_ready` | Output | Input queue can accept a request |
| `io_request_bits_tag` | Input, 32 bits | Task tag, returned unchanged |
| `io_request_bits_words_0` through `_11` | Input, 64 bits each | 12 field elements for one complete permutation |
| `io_response_valid` | Output | Complete result is valid |
| `io_response_ready` | Input | Downstream can accept the result |
| `io_response_bits_tag` | Output, 32 bits | Corresponding task tag |
| `io_response_bits_words_0` through `_11` | Output, 64 bits each | 12 permuted field elements |

A transfer occurs if and only if the corresponding `valid && ready` is true. Elements are not transferred partially; there is no additional start pulse or SoC control register. Under request backpressure, the sender must hold `valid`, all data, and the tag stable; the circuit similarly holds a backpressured response stable.

Use handshakes rather than assuming a fixed latency. Tags associate requests with responses. Reset invalidates all in-flight transactions, which the caller must resubmit. Tags need not be consecutive or unique; tests use consecutive unique tags to detect missing or duplicate responses.

`io_enable=0` only prevents starting new tasks from the input queue: it does not clear the queue, revoke accepted requests, or freeze active computations. Up to 2 requests can still be buffered while starts are disabled; processing resumes when enabled. Sustained output backpressure eventually stalls scheduling and input admission, so acceptance is not unbounded. Performance tests use continuous input and output availability with `io_enable=1`; protocol backpressure is not included in performance figures.

The Chisel class also declares an auxiliary `busy` status. The fixed product SV extracted from the verification layer omits this unused output after generation-time pruning. The external contract of the bundled SV and scripted reproduction is the table above; integrations should not depend on `io_busy`. Generating directly from the Chisel class while retaining that port changes the product interface and synthesis input; validate and synthesize that generated configuration separately.
