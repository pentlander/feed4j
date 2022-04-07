@import com.pentlander.feed4j.Main.FeedItem
@import java.util.List
@param List<FeedItem> items

<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Rss Feed</title>
    <link href="index.css" rel="stylesheet">
</head>
<body class="bg-gray-100 dark:bg-gray-900">
<main class="max-w-4xl m-auto mt-3 p-3">
    <h1 class="font-bold text-5xl mb-5 text-gray-900 dark:text-gray-100">News Feed</h1>
    <ol class="list-outside">
        @for(var item : items)
        <li class="mb-3 flex flex-col">
            <a href="${item.uri().toString()}"
               class="font-semibold text-xl text-blue-500 visited:text-purple-500 visited:font-normal">${item.title()}</a>
            <p class="text-gray-800 dark:text-gray-200">${item.host()}</p>
        </li>
        @endfor
    </ol>
</main>
</body>
</html>
