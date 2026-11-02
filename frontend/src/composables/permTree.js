export const TYPE_LABEL = { directory: '目录', menu: '菜单', button: '按钮' }

/** Flatten a permission tree depth-first, annotating each node with its depth. */
export function flattenTree(nodes, depth = 0, out = []) {
  nodes.forEach(n => {
    out.push({ ...n, depth })
    flattenTree(n.children || [], depth + 1, out)
  })
  return out
}

/** Every id below (and not including) the given node. */
export function descendantIds(node) {
  const ids = []
  const walk = list => list.forEach(c => { ids.push(c.id); walk(c.children || []) })
  walk(node.children || [])
  return ids
}

/** How many levels the tree may have; the root is level 1. The backend enforces the same limit. */
export const MAX_DEPTH = 5

/**
 * What kinds of node may be created under a parent (null = at the root). Any type may sit under any node; the only
 * limit is the depth. `parent.depth` is 0 for a root, as set by flattenTree, so a node on level MAX_DEPTH has depth
 * MAX_DEPTH - 1 and can hold nothing more.
 */
export function childTypes(parent) {
  if (parent && (parent.depth ?? 0) + 1 >= MAX_DEPTH) return []
  return ['directory', 'menu', 'button']
}
