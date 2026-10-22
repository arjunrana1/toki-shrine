# TS-config-values — owner checks

Run on the first installed build that contains D1, which is expected to be the renamed Time Shrine build. Record each result with the date and the **installed APK hash**. Not installed yet; no results.

| ID | What to test | Fail | Pass |
|---|---|---|---|
| CV-O1 | New block → step 3, Type a passage | Starts anywhere but 40; goes below 30 or above 150 | Starts at 40 characters; − stops at 30, + stops at 150, steps of 10 |
| CV-O2 | New block → step 3, Wait it out | Starts anywhere but 30 s; goes below 30 s or above 2 min | Starts at 30 sec; − stops at 30 sec, + stops at 2 min, steps of 10 s |
| CV-O3 | Step 4 on the debug build (typing, then waiting) | Wrong rungs or preselection | Typing 20 / 200 / 300 with 20 preselected; waiting 20 seconds / 2 minutes / 4 minutes with 20 s preselected; "Recommended" on the middle rung |
| CV-O4 | Typing passages | Repeated word back to back; odd or non-words | Passages read as varied everyday words |
