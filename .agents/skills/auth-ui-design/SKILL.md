---
name: auth-ui-design
description: >
  Design, restyle or edit the login, signup, logout and password-reset pages of a Halo site that
  runs the auth-ui plugin, through Halo MCP Server tools (auth-ui__*). Use when the user asks
  for a new login page look, a themed or animated sign-in, brand panel copy, or changes to an
  existing AuthTemplate design. Not for plugin source changes; use halo-plugin-dev for those.
---

# Auth UI Design

Designs are `AuthTemplate` records. Halo still renders and processes every form, so a design
only changes the presentation. Work through the MCP tools, not the plugin source.

## Before You Start

- Check that the `auth-ui__*` tools are available. If they are not, the user must:
  1. install and enable the MCP Server plugin (1.3.0 or newer);
  2. open 「工具 → MCP 服务」;
  3. add the 登录界面设计 tools to an access key.

  Tools are not added to existing keys automatically.
- Call `auth-ui__get_design_guide` once per session. It is the authoritative reference: page
  structure, CSS variables, slot HTML rules, motion keywords and the `AuthUI` JavaScript API.
  Do not rely on memory for keywords or selectors. It also repeats the Designing, Saving and
  Report sections below, so agents without this skill installed follow the same workflow.
- Call `auth-ui__list_designs` to see what exists and which design is active.

## Designing

1. Use the lowest layer that does the job, in this order:
   - `theme` for colours, layout, background and motion;
   - `slots` for brand copy and badges;
   - `code.css` for details the theme cannot express;
   - `code.js` only for behaviour.
2. Design for both places a visitor sees the page.
   - Most visitors use the site modal (`data-embedded=true`). There the backdrop, the brand
     panel and the ambient motion are hidden.
   - The full page shows everything.

   The card, form and header slot must look right on their own.
3. Keep contrast readable. Set `text`, `muted`, `surface`, `field` and `border` together
   whenever `colorScheme` is `dark` or `surface` is changed.
4. Keep motion purposeful. Use one entrance and at most one ambient effect. Any custom
   `@keyframes` needs a `prefers-reduced-motion` rule.
5. Never hide or restyle Halo's inputs out of usability. Do not use `code.js` to change form
   fields, names or actions.

## Saving

1. Run `auth-ui__lint_design` on the draft spec. Fix every warning, then lint again.
2. Save the design.
   - New design: call `auth-ui__create_design`.
   - Existing design: call `auth-ui__get_design` first, then `auth-ui__update_design` with
     the full spec and the `version` you read.

   An update replaces the whole spec, so keep every field you did not mean to change. On a
   `CONFLICT` error, read the design again and redo the edit; do not force it.
3. Call `auth-ui__preview_design` and give the user the studio link and the login preview links
   (page and modal). Previews need a signed-in plugin manager, and `code.js` never runs in
   previews.
4. Activate only with explicit confirmation. `auth-ui__activate_design` changes the live login
   for every visitor. Updating the active design is also live immediately, so say so before
   editing it. To roll back, activate the previous design or pass an empty name for the base
   look.
5. Delete only designs the user names. Deleting the active design returns the site to the base
   look.

## Report

Tell the user:

- the design name and what changed;
- any lint warnings you chose to keep, and why;
- the preview links;
- whether the design is live.
