# D-017: Open photos and play videos in the ticket detail

Type: bug · Reported by Francisco during testing (2026-11-21), item 8.

## Problem
Videos attached to a ticket cannot be watched. The detail (ShowScreen, used by citizens and
operators) shows each attachment as a 96 dp tile with a play badge, but the tile does nothing
when tapped.

## Brief
I.1.C / I.2 / I.3: the detail shows the ticket's "imagem(ns) e/ou vídeo(s)"; II.4: the
resolution proof is a "fotografia e/ou vídeo comprovativo".

## Acceptance
- Tapping a video tile in the detail plays the video, with play/pause and seek.
- Tapping a photo tile shows it larger.
- Works for report media and for the operator's resolution proof, in portrait and landscape.
