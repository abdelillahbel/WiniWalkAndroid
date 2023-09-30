const simpleGit = require('simple-git');
const fs = require('fs');

const git = simpleGit();

async function createCommitAndPush() {
    const remote = 'origin'; // Change this to match your remote name
    const branch = 'master'; // Change this to your desired branch
    const fileName = 'assets/file.txt'; // Use the same file for all commits

    // Infinite loop to keep making commits and pushes
    while (true) {
        try {
            // Generate a random commit message
            const commitMessage = `Commit ${new Date().toISOString()}`;

            // Update the content of the existing file
            fs.appendFileSync(fileName, `\n${commitMessage}`);

            // Add, commit, and push the changes
            await git.add(fileName);
            await git.commit(commitMessage);
            await git.push(remote, branch);

            console.log(`Commit and push: ${commitMessage}`);

            // Wait for 1 second before the next commit
            await new Promise((resolve) => setTimeout(resolve, 100));
        } catch (error) {
            console.error(`Error creating commit and pushing: ${error}`);
        }
    }
}

createCommitAndPush();
