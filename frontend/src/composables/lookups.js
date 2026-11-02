import { reactive } from 'vue'
import { deptApi } from '../api/depts'
import { platformApi } from '../api/platforms'
import { roleApi } from '../api/roles'
import { flattenTree } from './permTree'

export const lookups = reactive({ roles: [], platforms: [], depts: [] })

export async function loadRoles() {
  lookups.roles = await roleApi.list()
}

export async function loadPlatforms() {
  lookups.platforms = await platformApi.list()
}

/** Departments as a flat list in tree order; each entry has a `depth` (0 for a root), for indented pickers. */
export async function loadDepts() {
  lookups.depts = flattenTree(await deptApi.tree())
}
